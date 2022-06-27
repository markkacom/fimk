/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt;

import nxt.crypto.Crypto;
import nxt.util.*;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class Generator implements Comparable<Generator> {

    public enum Event {
        GENERATION_DEADLINE, START_FORGING, STOP_FORGING
    }

    public enum TimePolitic {
        NEAREST, MINIMAL
    }

    private static final byte[] fakeForgingPublicKey;
    static {
        byte[] publicKey = null;
        if (Nxt.getBooleanProperty("fimk.enableFakeForging")) {
            Account fakeForgingAccount = Account.getAccount(Convert.parseAccountId(Nxt.getStringProperty("fimk.fakeForgingAccount")));
            if (fakeForgingAccount != null) {
                publicKey = fakeForgingAccount.getPublicKey();
            }
        }
        fakeForgingPublicKey = publicKey;
    }

    private static final Listeners<Generator,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, Generator> generators = new ConcurrentHashMap<>();
    private static final Collection<Generator> allGenerators = Collections.unmodifiableCollection(generators.values());
    private static volatile List<Generator> sortedForgers = null;
    private static long lastBlockId;
    private static int delayTime = Constants.FORGING_DELAY;

    private static final Runnable generateBlocksThread = new Runnable() {

        private volatile boolean logged;

        @Override
        public void run() {

            try {
                /* XXX - Enable forging below Constants.TRANSPARENT_FORGING_BLOCK */
                try {
                    synchronized (Nxt.getBlockchain()) {
                        Block lastBlock = Nxt.getBlockchain().getLastBlock();
                        if (lastBlock == null || lastBlock.getHeight() < Constants.LAST_KNOWN_BLOCK) {
                            return;
                        }

                        if (lastBlock.getId() != lastBlockId || sortedForgers == null) {
                            lastBlockId = lastBlock.getId();
                            List<Generator> forgers = new ArrayList<>();
                            for (Generator generator : generators.values()) {
                                generator.updateHit(lastBlock, TimePolitic.NEAREST);
                                if (generator.effectiveBalance.signum() > 0) {
                                    forgers.add(generator);
                                }
                            }
                            Collections.sort(forgers);
                            sortedForgers = Collections.unmodifiableList(forgers);
                            logged = false;
                        }
                        int generationLimit = Nxt.getEpochTime() - delayTime;
                        log(generationLimit, lastBlock);
                        for (Generator generator : sortedForgers) {
                            if (Nxt.getEpochTime() < generator.pauseEnd) return;

                            int blockTimestamp = (generationLimit - generator.hitTime > 3600) ? generationLimit : (int) generator.hitTime + 1;
                            BlockchainProcessor.PreparedBlockTransactions preparedBlockTransactions =
                                    BlockchainProcessorImpl.getInstance().prepareUnconfirmedTransactions(blockTimestamp, lastBlock.getHeight());
                            if (!preparedBlockTransactions.unconfirmedTransactions.isEmpty()) {
                                generator.updateHit(lastBlock, TimePolitic.MINIMAL);
                                blockTimestamp = (generationLimit - generator.hitTime > 3600) ? generationLimit : (int) generator.hitTime + 1;
                                BlockchainProcessor.PreparedBlockTransactions preparedBlockTransactions2 =
                                        BlockchainProcessorImpl.getInstance().prepareUnconfirmedTransactions(blockTimestamp, lastBlock.getHeight());
                                if (preparedBlockTransactions2.unconfirmedTransactions.isEmpty()) {
                                    //revert to standard hit
                                    generator.updateHit(lastBlock, TimePolitic.NEAREST);
                                } else {
                                    preparedBlockTransactions = preparedBlockTransactions2;
                                }
                            }

                            if (generator.getHitTime() > generationLimit) return;
                            generator.pauseEnd = Nxt.getEpochTime() + Constants.SECONDS_BETWEEN_BLOCKS;
                            if (generator.forge(lastBlock, generationLimit, preparedBlockTransactions)) {
                                generator.pauseEnd = 0; //success forged block, so no pause
                                return;
                            }
                        }
                    } // synchronized
                } catch (Exception e) {
                    Logger.logMessage("Error in block generation thread", e);
                }
            } catch (Throwable t) {
                Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

        private void log(int generationLimit, Block lastBlock) {
            if (!logged) {
                for (Generator generator : sortedForgers) {
                    if (generator.getHitTime() - generationLimit > 60) {
                        break;
                    }
                    Logger.logDebugMessage(generator.toString());
                    logged = true;
                }
                if (logged && Logger.isDebugEnabled()) {
                    //report target state
                    double diff = (double) lastBlock.getBaseTarget() / Constants.INITIAL_BASE_TARGET;
                    int pos = (int) (diff * 50);
                    String filled = pos <= 0 ? "" : String.format("%1$" + pos + "s", "").replace(' ', '=');
                    String padded = filled.length() >= 100 ? "" : String.format("%1$" + (100 - filled.length()) + "s", "").replace(' ', '.');
                    StringBuilder sb = new StringBuilder(filled + padded);
                    sb.setCharAt(50, '|');
                    Logger.logDebugMessage("target [" + sb + "]");
                }
            }
        }

    };

    static {
        ThreadPool.scheduleThread("GenerateBlocks", generateBlocksThread, 500, TimeUnit.MILLISECONDS);
    }

    static void init() {}

    public static boolean addListener(Listener<Generator> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Generator> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static Generator startForging(String secretPhrase) {
        Generator generator = new Generator(secretPhrase);

        /* XXX - Prevent or allow forging based on fimk.allowedToForge */
        if (Constants.allowedToForge instanceof List) {
            boolean found = false;
            for (Long allowed : Constants.allowedToForge) {
                if (allowed.equals(generator.getAccountId())) {
                    found = true;
                    break;
                }
            }
            if (found == false) {
                Logger.logDebugMessage("Account " + Long.toUnsignedString(generator.getAccountId()) +
                    " is not allowed to forge. See fimk.allowedToForge property.");
                return null;
            }
        }

        if (AccountColor.getAccountColorEnabled()) {
            Account account = Account.getAccount(generator.getAccountId());
            if (account.getAccountColorId() != 0) {
                Logger.logDebugMessage("Account " + Long.toUnsignedString(generator.getAccountId()) +
                    " is not allowed to forge. Only non colored accounts can forge.");
                return null;
            }
        }

        Generator old = generators.putIfAbsent(secretPhrase, generator);
        if (old != null) {
            Logger.logDebugMessage(old + " is already forging");
            return old;
        }
        listeners.notify(generator, Event.START_FORGING);
        Logger.logDebugMessage(generator + " started");
        return generator;
    }

    public static Generator stopForging(String secretPhrase) {
        Generator generator = generators.remove(secretPhrase);
        if (generator != null) {
            sortedForgers = null;
            Logger.logDebugMessage(generator + " stopped");
            listeners.notify(generator, Event.STOP_FORGING);
        }
        return generator;
    }

    public static int stopForging() {
        int count = generators.size();
        Iterator<Generator> iter = generators.values().iterator();
        while (iter.hasNext()) {
            Generator generator = iter.next();
            iter.remove();
            Logger.logDebugMessage(generator + " stopped");
            listeners.notify(generator, Event.STOP_FORGING);
        }
        sortedForgers = null;
        return count;
    }

    public static Generator getGenerator(String secretPhrase) {
        return generators.get(secretPhrase);
    }

    public static Generator getGenerator(long id) {
        for (Generator generator : getAllGenerators()) {
            if (generator.accountId == id) {
                return generator;
            }
        }
        return null;
    }

    public static Collection<Generator> getAllGenerators() {
        return allGenerators;
    }

    public static List<Generator> getSortedForgers() {
        return sortedForgers == null ? Collections.<Generator>emptyList() : sortedForgers;
    }

    public static long getNextHitTime(long lastBlockId, int curTime) {
        synchronized (Nxt.getBlockchain()) {
            if (lastBlockId == Generator.lastBlockId && sortedForgers != null) {
                for (Generator generator : sortedForgers) {
                    if (generator.getHitTime() >= curTime - Constants.FORGING_DELAY) {
                        return generator.getHitTime();
                    }
                }
            }
            return 0;
        }
    }

    static void setDelay(int delay) {
        synchronized (Nxt.getBlockchain()) {
            Generator.delayTime = delay;
        }
    }

    static boolean verifyHit(BigInteger[] hits, int selectedHitIndex, BigInteger effectiveBalance, Block previousBlock, int timestamp) {
        int elapsedTime = timestamp - previousBlock.getTimestamp();
        if (elapsedTime <= 0) {
            return false;
        }
        BigInteger effectiveBaseTarget = BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(effectiveBalance);
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
        return hits[selectedHitIndex].compareTo(target) < 0
                && (previousBlock.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_8
                || hits[selectedHitIndex].compareTo(prevTarget) >= 0
                || (Constants.isTestnet ? elapsedTime > 300 : elapsedTime > 3600)
                || Constants.isOffline);
    }

    static boolean allowsFakeForging(byte[] publicKey) {
        return Constants.isTestnet && publicKey != null && Arrays.equals(publicKey, fakeForgingPublicKey);
    }

    static BigInteger[] calculateHits(byte[] publicKey, Block block) {
        if (allowsFakeForging(publicKey)) {
            return new BigInteger[]{BigInteger.ZERO};
        }
        /* XXX - Enable forging below TRANSPARENT_FORGING_BLOCK */
        //if (block.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK) {
        //    throw new IllegalArgumentException("Not supported below Transparent Forging Block");
        //}

        MessageDigest digest = Crypto.sha256();
        digest.update(block.getGenerationSignature());
        BigInteger[] result = new BigInteger[117];
        byte[] generationSignatureHash = digest.digest(publicKey);
        result[0] = new BigInteger(1, new byte[]{
                generationSignatureHash[7],
                generationSignatureHash[6],
                generationSignatureHash[5],
                generationSignatureHash[4],
                generationSignatureHash[3],
                generationSignatureHash[2],
                generationSignatureHash[1],
                generationSignatureHash[0]
        });
        byte[] digestSource = generationSignatureHash;
        for (int i = 1; i < result.length; i++) {
            byte[] nextHash = digest.digest(digestSource);
            result[i] = new BigInteger(1, new byte[]{
                    nextHash[7],
                    nextHash[6],
                    nextHash[5],
                    nextHash[4],
                    nextHash[3],
                    nextHash[2],
                    nextHash[1],
                    nextHash[0]
            });
            digestSource = nextHash;
        }
        return result;
    }

    static long[] calculateHitTime(Account account, Block block, TimePolitic timePolitic) {
        return calculateHitTime(
                BigInteger.valueOf(account.getEffectiveBalanceNXT(block.getHeight())),
                calculateHits(account.getPublicKey(), block),
                block,
                timePolitic
        );
    }

    static long[] calculateHitTime(BigInteger effectiveBalance, BigInteger[] hits, Block block, TimePolitic timePolitic) {
        // blockTimestamp + hit / (block.baseTarget * effectiveBalance)
//        return block.getTimestamp()
//                + hit.divide(BigInteger.valueOf(block.getBaseTarget()).multiply(effectiveBalance)).longValue();

        long candidateInterval;
        long interval = Long.MAX_VALUE;
        int index = -1;
        long diff = Long.MAX_VALUE;
        BigInteger divider = BigInteger.valueOf(block.getBaseTarget()).multiply(effectiveBalance);
        for (int i = 0; i < hits.length; i++) {
            BigInteger hit = hits[i];
            candidateInterval = hit.divide(divider).longValue();
            if (candidateInterval <= 0) continue;

            if (timePolitic == TimePolitic.NEAREST) {
                if (Math.abs(Constants.SECONDS_BETWEEN_BLOCKS - candidateInterval) < diff) {
                    diff = Math.abs(Constants.SECONDS_BETWEEN_BLOCKS - candidateInterval);
                    interval = candidateInterval;
                    index = i;
                }
            }

            if (timePolitic == TimePolitic.MINIMAL) {
                if (candidateInterval < interval) {
                    interval = candidateInterval;
                    index = i;
                }
            }
        }
        return new long[]{block.getTimestamp() + interval, index};
    }


    private final long accountId;
    private final String secretPhrase;
    private final byte[] publicKey;
    private volatile long hitTime;
    private volatile BigInteger[] hits;
    private volatile int selectedHitIndex;
    private volatile BigInteger effectiveBalance;
    private volatile int pauseEnd = 0;

    private Generator(String secretPhrase) {
        this.secretPhrase = secretPhrase;
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.accountId = Account.getId(publicKey);
        if (Nxt.getBlockchain().getHeight() >= Constants.LAST_KNOWN_BLOCK) {
            updateHit(Nxt.getBlockchain().getLastBlock(), TimePolitic.NEAREST);
        }
        sortedForgers = null;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getDeadline() {
        return Math.max(hitTime - Nxt.getBlockchain().getLastBlock().getTimestamp(), 0);
    }

    public long getHitTime() {
        return hitTime;
    }

    @Override
    public int compareTo(Generator g) {
        //todo confused effectiveBalance between this and other (g)
        int i = this.hits[selectedHitIndex].multiply(g.effectiveBalance).compareTo(g.hits[g.selectedHitIndex].multiply(this.effectiveBalance));
        if (i != 0) {
            return i;
        }
        return Long.compare(accountId, g.accountId);
    }

    @Override
    public String toString() {
        return "Forger started " + Long.toUnsignedString(accountId) + " ETA " + getDeadline() + "s hittime " + hitTime;
    }

    private void updateHit(Block lastBlock, TimePolitic tm) {
        Account account = Account.getAccount(accountId);
        effectiveBalance = BigInteger.valueOf(account == null || account.getEffectiveBalanceNXT() <= 0 ? 0 : account.getEffectiveBalanceNXT());
        if (effectiveBalance.signum() == 0) {
            return;
        }
        hits = calculateHits(publicKey, lastBlock);

        long[] hitTimeAndIndex = calculateHitTime(effectiveBalance, hits, lastBlock, tm);
        //if (hitTime != hitTimeAndIndex[0]) System.out.println("delta " + (hitTimeAndIndex[0]- hitTime));
        hitTime = hitTimeAndIndex[0];
        selectedHitIndex = (int) hitTimeAndIndex[1];
        listeners.notify(this, Event.GENERATION_DEADLINE);
    }

    boolean forge(Block lastBlock, int generationLimit, BlockchainProcessor.PreparedBlockTransactions unconfirmedTransactions)
            throws BlockchainProcessor.BlockNotAcceptedException {
        int timestamp = (generationLimit - hitTime > 3600) ? generationLimit : (int)hitTime + 1;
        if (!verifyHit(hits, selectedHitIndex, effectiveBalance, lastBlock, timestamp)) {
            Logger.logDebugMessage(this + " failed to forge at " + timestamp);
            return false;
        }
        int start = Nxt.getEpochTime();
        while (true) {
            try {
                BlockchainProcessorImpl.getInstance().generateBlock(secretPhrase, timestamp, unconfirmedTransactions);
                setDelay(Constants.FORGING_DELAY);
                return true;
            } catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                // the bad transaction has been expunged, try again
                if (Nxt.getEpochTime() - start > 10) { // give up after trying for 10 s
                    throw e;
                }
            }
        }
    }

}
