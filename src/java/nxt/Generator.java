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
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class Generator implements Comparable<Generator> {

    public enum Event {
        GENERATION_DEADLINE, START_FORGING, STOP_FORGING
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
    private static int delayTime = 0;

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
                                generator.setLastBlock(lastBlock);
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
                            if (generator.getHitTime() > generationLimit) return;
                            if (generator.forge(lastBlock, generationLimit)) return;
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
                    int pos = (int) (diff * 30);
                    String filled = pos <= 0 ? "" : String.format("%1$" + Math.min(pos, 60) + "s", "").replace(' ', '=');
                    String padded = filled.length() >= 60 ? "" : String.format("%1$" + (60 - filled.length()) + "s", "").replace(' ', '.');
                    StringBuilder sb = new StringBuilder(filled + padded);
                    sb.setCharAt(30, '|');
                    Logger.logDebugMessage("target [" + sb.append("]").append(" x").append(String.format("%.3f", diff)));
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
                    if (generator.getHitTime() >= curTime) {
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

    static String verifyHit(BigInteger[] hits, int selectedHitIndex, BigInteger effectiveBalance, Block previousBlock, int timestamp) {
        int interval = timestamp - previousBlock.getTimestamp();
        if (interval <= 0)  return "Elapsed time is not positive " + interval;

        // hit time calculation:  hit / (block.baseTarget * effectiveBalance) and then +1
        // so   hit = (elapsedTime - 1) * (block.baseTarget * effectiveBalance)  but we dont to get hit back due the loss of accuracy during rounding

        int expectedInterval = hits[selectedHitIndex].divide(
                BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(effectiveBalance)).intValue() + 1;

        if (expectedInterval > interval) return String.format("Expected interval %s less than fact interval %s", expectedInterval, interval);

        if (previousBlock.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_8
                        || interval > Constants.BLOCK_INTERVAL_THRESHOLD
                        || Constants.isOffline) return null;

        if (expectedInterval != interval) return String.format("Expected interval %s is not equal fact interval %s", expectedInterval, interval);
        return null;
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
        BigInteger[] result = new BigInteger[block.getHeight() < Constants.CONTROL_FORGING_TIME_BLOCK ? 1 : 17];
        byte[] hash = digest.digest(publicKey);
        result[0] = new BigInteger(1, new byte[]{
                hash[7],
                hash[6],
                hash[5],
                hash[4],
                hash[3],
                hash[2],
                hash[1],
                hash[0]
        });

        /*
        byte[] a1 = new byte[generationSignatureHash.length + 1] ;
        System.arraycopy(generationSignatureHash, 0, a1, 1, generationSignatureHash.length);
        byte[] a2 = new byte[generationSignatureHash.length + 1] ;
        System.arraycopy(generationSignatureHash, 0, a2, 1, generationSignatureHash.length);
        a2[0] = 1;
        byte[] a3 = new byte[generationSignatureHash.length + 1] ;
        System.arraycopy(generationSignatureHash, 0, a3, 1, generationSignatureHash.length);
        a3[0] = 2;
        //...
        // todo 8 byte arrays for parallel digest computation (optimal for cpu 8 cores).
        //  Warning: result of parallel computation MUST have the same order (sync results): result1 concat result2 concat result3...
        */

        byte[] digestSource = hash;
        for (int i = 1; i < result.length; i++) {
            byte[] h = digest.digest(digestSource);
            result[i] = new BigInteger(1, new byte[]{h[7], h[6], h[5], h[4], h[3], h[2], h[1], h[0]});
            digestSource = h;
        }
        return result;
    }

    static long[] calculateHitTime(Account account, Block block) {
        return calculateHitTime(
                BigInteger.valueOf(account.getEffectiveBalanceNXT(block.getHeight())),
                calculateHits(account.getPublicKey(), block),
                block
        );
    }

    static long[] calculateHitTime(BigInteger effectiveBalance, BigInteger[] hits, Block block) {
        // blockTimestamp + hit / (block.baseTarget * effectiveBalance)

        /* Desired interval is Constants.SECONDS_BETWEEN_BLOCKS. Select two hits that provide nearest to desired the hit time.
        If previous interval is less the desired interval the hit time
        */

        int candidateInterval;
        int interval = Constants.SECONDS_BETWEEN_BLOCKS;
        int altInterval = interval;  // altInterval and altIndex are the second selected ones
        int index = -1;
        int altIndex = -1;
        int diff = Integer.MAX_VALUE;
        BigInteger divider = BigInteger.valueOf(block.getBaseTarget()).multiply(effectiveBalance);

        //in fact the desired interval is (Constants.SECONDS_BETWEEN_BLOCKS - 1), this leads to 30s between blocks and stabilised base target
        int desiredInterval = Math.abs(Nxt.getBlockchain().desiredBlockInterval(block)) - 1;

        for (int i = 0; i < hits.length; i++) {
            BigInteger hit = hits[i];
            candidateInterval = hit.divide(divider).intValue();
            int d = Math.abs(desiredInterval - candidateInterval);
            if (d < diff) {
                diff = d;
                altInterval = interval;
                altIndex = index;
                interval = candidateInterval;
                index = i;
            }
        }

        if (block.getHeight() > Constants.CONTROL_FORGING_TUNED_HITTIME_BLOCK && altIndex != -1) {
            Block preBlock = Nxt.getBlockchain().getBlock(block.getPreviousBlockId());
            int preDesiredInterval = Math.abs(Nxt.getBlockchain().desiredBlockInterval(preBlock)) - 1;
            if (preBlock != null) {
                int preInterval = block.getTimestamp() - preBlock.getTimestamp();
                if ((preInterval > preDesiredInterval && interval > desiredInterval && altInterval < interval)
                        || (preInterval < preDesiredInterval && interval < desiredInterval && altInterval > interval)) {
                    interval = altInterval;
                    index = altIndex;
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

    private Generator(String secretPhrase) {
        this.secretPhrase = secretPhrase;
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.accountId = Account.getId(publicKey);
        if (Nxt.getBlockchain().getHeight() >= Constants.LAST_KNOWN_BLOCK) {
            setLastBlock(Nxt.getBlockchain().getLastBlock());
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

    private void setLastBlock(Block lastBlock) {
        Account account = Account.getAccount(accountId);
        effectiveBalance = BigInteger.valueOf(account == null || account.getEffectiveBalanceNXT() <= 0 ? 0 : account.getEffectiveBalanceNXT());
        if (effectiveBalance.signum() == 0) {
            return;
        }
        hits = calculateHits(publicKey, lastBlock);
        long[] hitTimeAndIndex = calculateHitTime(effectiveBalance, hits, lastBlock);
        hitTime = hitTimeAndIndex[0];
        selectedHitIndex = (int) hitTimeAndIndex[1];
        listeners.notify(this, Event.GENERATION_DEADLINE);
    }

    boolean forge(Block lastBlock, int generationLimit) throws BlockchainProcessor.BlockNotAcceptedException {
        int timestamp = (generationLimit - hitTime > Constants.BLOCK_INTERVAL_THRESHOLD) ? generationLimit : (int) hitTime + 1;
        String hitVerifyingResult = verifyHit(hits, selectedHitIndex, effectiveBalance, lastBlock, timestamp);
        if (hitVerifyingResult != null) {
            Logger.logDebugMessage(this + " failed to forge at " + timestamp + ". Hit is wrong: " + hitVerifyingResult);
            return false;
        }
        int start = Nxt.getEpochTime();
        while (true) {
            try {
                BlockchainProcessorImpl.getInstance().generateBlock(secretPhrase, timestamp);
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
