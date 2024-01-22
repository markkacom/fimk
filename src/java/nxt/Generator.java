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
                    Logger.logDebugMessage(
                            String.format("%s target x%.3f",
                                    generator,
                                    (double) lastBlock.getBaseTarget() / Constants.INITIAL_BASE_TARGET
                            ));
                    logged = true;
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

    /**
     *
     * @param secretPhrase
     * @return instance of Generator or error string
     */
    public static Object startForging(String secretPhrase) {
        synchronized (generators) {
            if (!generators.isEmpty()) {
                Generator alreadyGenerator = generators.values().stream().findFirst().get();
                return String.format("Only one working forger is allowed. The place is already occupied by forger %s.",
                        Convert.rsAccount(alreadyGenerator.getAccountId()));
            }
        }

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
            if (!found) {
                String message = "Account " + Long.toUnsignedString(generator.getAccountId()) +
                        " is not allowed to forge.";
                Logger.logDebugMessage(message + " See fimk.allowedToForge property.");
                return message;
            }
        }

        Account account = Account.getAccount(generator.getAccountId());

        if (AccountColor.getAccountColorEnabled()) {
            if (account.getAccountColorId() != 0) {
                String message = "Account " + Long.toUnsignedString(generator.getAccountId()) +
                        " is not allowed to forge. Only non colored accounts can forge.";
                Logger.logDebugMessage(message);
                return message;
            }
        }

        if (!isBalanceEnough(Nxt.getBlockchain().getHeight(), generator.effectiveBalance)) {
            String message = String.format("Effective balance %d less than %d is not enough for forging",
                    generator.effectiveBalance.longValue(), Constants.FORGING_THRESHOLD.longValue());
            Logger.logDebugMessage(message);
            return message;
        }

        Generator old = generators.putIfAbsent(secretPhrase, generator);
        if (old != null) {
            String message = old + " is already forging";
            Logger.logDebugMessage(message);
            return message;
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
        if (!isBalanceEnough(previousBlock.getHeight(), effectiveBalance)) {
            return String.format("Effective balance %d less than %d is not enough for forging",
                    effectiveBalance.longValue(), Constants.FORGING_THRESHOLD.longValue());
        }

        // hit time calculation:  hit / (block.baseTarget * effectiveBalance) and then +1
        // so   hit = (elapsedTime - 1) * (block.baseTarget * effectiveBalance)  but we dont to get hit back due the loss of accuracy during rounding

        int expectedInterval = hits[selectedHitIndex].divide(
                BigInteger.valueOf(previousBlock.getBaseTarget()).multiply(forgingBalance(effectiveBalance, previousBlock.getHeight()))).intValue() + 1;

        if (expectedInterval > interval) return String.format("Fact interval %s less than expected interval %s", interval, expectedInterval);

        if (previousBlock.getHeight() < Constants.TRANSPARENT_FORGING_BLOCK_8
                        || interval > Constants.BLOCK_INTERVAL_THRESHOLD
                        || Constants.isOffline) return null;

        if (expectedInterval != interval) return String.format("Expected interval %s is not equal fact interval %s", expectedInterval, interval);
        return null;
    }

    static boolean isBalanceEnough(int height, BigInteger effectiveBalance) {
        return height <= Constants.FORGING_BALANCE_BLOCK || effectiveBalance.compareTo(Constants.FORGING_THRESHOLD) >= 0;
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
        int hitNum = 1;
        if (block.getHeight() >= Constants.FORGING_BALANCE_BLOCK) {
            hitNum = Constants.HITS_NUMBER_2;
        } else if (block.getHeight() >= Constants.CONTROL_FORGING_TIME_BLOCK) {
            hitNum = Constants.HITS_NUMBER;
        }
        BigInteger[] result = new BigInteger[hitNum];
        byte[] h = digest.digest(publicKey);
        result[0] = new BigInteger(1, new byte[]{h[7], h[6], h[5], h[4], h[3], h[2], h[1], h[0]});

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

        byte[] digestSource = h;
        for (int i = 1; i < result.length; i++) {
            byte[] hash = digest.digest(digestSource);
            result[i] = new BigInteger(1, new byte[]{hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            digestSource = hash;
        }
        return result;
    }

    static long[] calculateHitTime(byte[] publicKey, long effectiveBalance, Block block) {
        return calculateHitTime(
                BigInteger.valueOf(effectiveBalance),
                calculateHits(publicKey, block),
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
        BigInteger divider = BigInteger.valueOf(block.getBaseTarget()).multiply(forgingBalance(effectiveBalance, block.getHeight()));

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

        if (block.getHeight() > Constants.CONTROL_FORGING_TIME_BLOCK && altIndex != -1) {
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

    private static BigInteger forgingBalance(BigInteger balance, int height) {
        if (height < Constants.FORGING_BALANCE_BLOCK) return balance;
        return BigInteger.valueOf((long) (Math.log10(balance.longValue()) * 334));
    }

    static boolean allowsFakeForging(byte[] publicKey) {
        return Constants.isTestnet && publicKey != null && Arrays.equals(publicKey, fakeForgingPublicKey);
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
