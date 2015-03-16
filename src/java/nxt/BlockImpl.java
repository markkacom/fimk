package nxt;

import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class BlockImpl implements Block {

    private final int version;
    private final int timestamp;
    private final long previousBlockId;
    private volatile byte[] generatorPublicKey;
    private final byte[] previousBlockHash;
    private final long totalAmountNQT;
    private final long totalFeeNQT;
    private final int payloadLength;
    private final byte[] generationSignature;
    private final byte[] payloadHash;
    private volatile List<TransactionImpl> blockTransactions;

    private byte[] blockSignature;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO;
    private long baseTarget = Constants.INITIAL_BASE_TARGET;
    private volatile long nextBlockId;
    private int height = -1;
    private volatile long id;
    private volatile String stringId = null;
    private volatile long generatorId;


    BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, byte[] previousBlockHash, List<TransactionImpl> transactions)
            throws NxtException.ValidationException {

        if (payloadLength > Constants.MAX_PAYLOAD_LENGTH || payloadLength < 0) {
            throw new NxtException.NotValidException("attempted to create a block with payloadLength " + payloadLength);
        }

        this.version = version;
        this.timestamp = timestamp;
        this.previousBlockId = previousBlockId;
        this.totalAmountNQT = totalAmountNQT;
        this.totalFeeNQT = totalFeeNQT;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generatorPublicKey = generatorPublicKey;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;
        this.previousBlockHash = previousBlockHash;
        if (transactions != null) {
            this.blockTransactions = Collections.unmodifiableList(transactions);
            if (blockTransactions.size() > Constants.MAX_NUMBER_OF_TRANSACTIONS) {
                throw new NxtException.NotValidException("attempted to create a block with " + blockTransactions.size() + " transactions");
            }
        }
    }

    BlockImpl(int version, int timestamp, long previousBlockId, long totalAmountNQT, long totalFeeNQT, int payloadLength,
              byte[] payloadHash, long generatorId, byte[] generationSignature, byte[] blockSignature,
              byte[] previousBlockHash, BigInteger cumulativeDifficulty, long baseTarget, long nextBlockId, int height, long id)
            throws NxtException.ValidationException {
        this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                null, generationSignature, blockSignature, previousBlockHash, null);
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.baseTarget = baseTarget;
        this.nextBlockId = nextBlockId;
        this.height = height;
        this.id = id;
        this.generatorId = generatorId;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public long getPreviousBlockId() {
        return previousBlockId;
    }

    @Override
    public byte[] getGeneratorPublicKey() {
        if (generatorPublicKey == null) {
            generatorPublicKey = Account.getPublicKey(generatorId);
        }
        return generatorPublicKey;
    }

    @Override
    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    @Override
    public long getTotalAmountNQT() {
        return totalAmountNQT;
    }

    @Override
    public long getTotalFeeNQT() {
        return totalFeeNQT;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public byte[] getPayloadHash() {
        return payloadHash;
    }

    @Override
    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    @Override
    public byte[] getBlockSignature() {
        return blockSignature;
    }

    @Override
    public List<TransactionImpl> getTransactions() {
        if (blockTransactions == null) {
            this.blockTransactions = Collections.unmodifiableList(TransactionDb.findBlockTransactions(getId()));
            for (TransactionImpl transaction : this.blockTransactions) {
                transaction.setBlock(this);
            }
        }
        return blockTransactions;
    }

    @Override
    public long getBaseTarget() {
        return baseTarget;
    }

    @Override
    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }

    @Override
    public long getNextBlockId() {
        return nextBlockId;
    }

    @Override
    public int getHeight() {
        if (height == -1) {
            throw new IllegalStateException("Block height not yet set");
        }
        return height;
    }

    @Override
    public long getId() {
        if (id == 0) {
            if (blockSignature == null) {
                throw new IllegalStateException("Block is not signed yet");
            }
            byte[] hash = Crypto.sha256().digest(getBytes());
            BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            id = bigInteger.longValue();
            stringId = bigInteger.toString();
        }
        return id;
    }

    @Override
    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Convert.toUnsignedLong(id);
            }
        }
        return stringId;
    }

    @Override
    public long getGeneratorId() {
        if (generatorId == 0) {
            generatorId = Account.getId(getGeneratorPublicKey());
        }
        return generatorId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockImpl && this.getId() == ((BlockImpl)o).getId();
    }

    @Override
    public int hashCode() {
        return (int)(getId() ^ (getId() >>> 32));
    }

    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("timestamp", timestamp);
        json.put("previousBlock", Convert.toUnsignedLong(previousBlockId));
        json.put("totalAmountNQT", totalAmountNQT);
        json.put("totalFeeNQT", totalFeeNQT);
        json.put("payloadLength", payloadLength);
        json.put("payloadHash", Convert.toHexString(payloadHash));
        json.put("generatorPublicKey", Convert.toHexString(getGeneratorPublicKey()));
        json.put("generationSignature", Convert.toHexString(generationSignature));
        if (version > 1) {
            json.put("previousBlockHash", Convert.toHexString(previousBlockHash));
        }
        json.put("blockSignature", Convert.toHexString(blockSignature));
        JSONArray transactionsData = new JSONArray();
        for (Transaction transaction : getTransactions()) {
            transactionsData.add(transaction.getJSONObject());
        }
        json.put("transactions", transactionsData);
        return json;
    }

    static BlockImpl parseBlock(JSONObject blockData) throws NxtException.ValidationException {
        try {
            int version = ((Long) blockData.get("version")).intValue();
            int timestamp = ((Long) blockData.get("timestamp")).intValue();
            long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            long totalAmountNQT = Convert.parseLong(blockData.get("totalAmountNQT"));
            long totalFeeNQT = Convert.parseLong(blockData.get("totalFeeNQT"));
            int payloadLength = ((Long) blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
            byte[] previousBlockHash = version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));
            List<TransactionImpl> blockTransactions = new ArrayList<>();
            for (Object transactionData : (JSONArray) blockData.get("transactions")) {
                blockTransactions.add(TransactionImpl.parseTransaction((JSONObject) transactionData));
            }
            return new BlockImpl(version, timestamp, previousBlock, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash, generatorPublicKey,
                    generationSignature, blockSignature, previousBlockHash, blockTransactions);
        } catch (NxtException.ValidationException|RuntimeException e) {
            Logger.logDebugMessage("Failed to parse block: " + blockData.toJSONString());
            throw e;
        }
    }

    byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + (version < 3 ? (4 + 4) : (8 + 8)) + 4 + 32 + 32 + (32 + 32) + 64);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version);
        buffer.putInt(timestamp);
        buffer.putLong(previousBlockId);
        buffer.putInt(getTransactions().size());
        if (version < 3) {
            buffer.putInt((int)(totalAmountNQT / Constants.ONE_NXT));
            buffer.putInt((int)(totalFeeNQT / Constants.ONE_NXT));
        } else {
            buffer.putLong(totalAmountNQT);
            buffer.putLong(totalFeeNQT);
        }
        buffer.putInt(payloadLength);
        buffer.put(payloadHash);
        buffer.put(getGeneratorPublicKey());
        buffer.put(generationSignature);
        if (version > 1) {
            buffer.put(previousBlockHash);
        }
        buffer.put(blockSignature);
        return buffer.array();
    }

    void sign(String secretPhrase) {
        if (blockSignature != null) {
            throw new IllegalStateException("Block already signed");
        }
        blockSignature = new byte[64];
        byte[] data = getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);
        blockSignature = Crypto.sign(data2, secretPhrase);
    }

    boolean verifyBlockSignature() {

        Account account = Account.getAccount(getGeneratorId());
        if (account == null) {
            return false;
        }

        byte[] data = getBytes();
        byte[] data2 = new byte[data.length - 64];
        System.arraycopy(data, 0, data2, 0, data2.length);

        return Crypto.verify(blockSignature, data2, getGeneratorPublicKey(), version >= 3) && account.setOrVerify(getGeneratorPublicKey());

    }
    
    /* XXX - fix for invalid generation signature block 96249 */
    final byte[] GENERATION_SIG_96248 = Convert.parseHexString("942b93195bcb48045019f38859606c1b4aefe98751dd97833631c7fab2c9edfd");
    final byte[] GENERATION_SIG_96249 = Convert.parseHexString("5f5c132acef36a1d329b69c45d1d26b3c3941e7679a6254ce825685100e04dd4"); 
    
    /* XXX - fix for invalid generation signature block 272974 */
    final byte[] GENERATION_SIG_272974 = Convert.parseHexString("147345e8e51e8d026c5e277cda8764e6a50abe763b583f38910572f810f7b7a3");
    final byte[] GENERATION_SIG_272975 = Convert.parseHexString("1a6ed2bcf9c9f70e169e76c6c260cfab72d95f8218e4fb6e91c106ea5d881b49");
    
    /* XXX - fix for invalid generation signature block 380560 */
    final byte[] GENERATION_SIG_380561 = Convert.parseHexString("d09af2f9aa2f200e80591d9b26bc3820165a19f51bc0041d230f885f12258c36");
    final byte[] GENERATION_SIG_380562 = Convert.parseHexString("a507753a79279800f543d018d789d75c1a2fd6137fd33c04323dcab9c2edabdf");    

    boolean verifyGenerationSignature() throws BlockchainProcessor.BlockOutOfOrderException {

        try {
            BlockImpl previousBlock = BlockchainImpl.getInstance().getBlock(getPreviousBlockId());
            if (previousBlock == null) {
                throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing");
            }

            if (version == 1 && !Crypto.verify(generationSignature, previousBlock.generationSignature, getGeneratorPublicKey(), version >= 3)) {
                return false;
            }

            Account account = Account.getAccount(getGeneratorId());
            long effectiveBalance = account == null ? 0 : account.getEffectiveBalanceNXT();
            if (effectiveBalance <= 0) {
                return false;
            }

            MessageDigest digest = Crypto.sha256();
            byte[] generationSignatureHash;
            if (version == 1) {
                generationSignatureHash = digest.digest(generationSignature);
            } else {
                digest.update(previousBlock.generationSignature);
                generationSignatureHash = digest.digest(getGeneratorPublicKey());
                if (!Arrays.equals(generationSignature, generationSignatureHash)) {
                    return false;
                }
            }
            
            /* XXX - Prevent stolen funds to forge blocks */
            if ( ! Locked.allowedToForge(account.getPublicKey())) {

              Logger.logMessage("Public key not allowed to forge blocks");
              return true;
            }
            
            /* XXX - fix for invalid generation signature block 96249 */
            if (previousBlock.height == 96248 && 
                Arrays.equals(GENERATION_SIG_96248, previousBlock.generationSignature) && 
                Arrays.equals(GENERATION_SIG_96249, generationSignature)) {
              
              Logger.logMessage("Block 96249 generation signature checkpoint passed");
              return true;
            }
            
            /* XXX - fix for invalid generation signature block 272974 */
            if (previousBlock.height == 272974 && 
                Arrays.equals(GENERATION_SIG_272974, previousBlock.generationSignature) && 
                Arrays.equals(GENERATION_SIG_272975, generationSignature)) {
              
              Logger.logMessage("Block 272974 generation signature checkpoint passed");
              return true;
            }
            
            /* XXX - fix for invalid generation signature block 380560 */
            if (previousBlock.height == 380561 && 
                Arrays.equals(GENERATION_SIG_380561, previousBlock.generationSignature) && 
                Arrays.equals(GENERATION_SIG_380562, generationSignature)) {
              
              Logger.logMessage("Block 380561 generation signature checkpoint passed");
              return true;
            }

            BigInteger hit = new BigInteger(1, new byte[]{generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});

            return Generator.verifyHit(hit, BigInteger.valueOf(effectiveBalance), previousBlock, timestamp)
                    || (this.height < Constants.TRANSPARENT_FORGING_BLOCK_5 /* NXT specific ** && Arrays.binarySearch(badBlocks, this.getId()) >= 0*/);

        } catch (RuntimeException e) {

            Logger.logMessage("Error verifying block generation signature", e);
            return false;

        }

    }

    /* NXT specific */
    /*private static final long[] badBlocks = new long[] {
            5113090348579089956L, 8032405266942971936L, 7702042872885598917L, -407022268390237559L, -3320029330888410250L,
            -6568770202903512165L, 4288642518741472722L, 5315076199486616536L, -6175599071600228543L};
    static {
        Arrays.sort(badBlocks);
    }*/

    void apply() throws BlockchainProcessor.TransactionNotAcceptedException {
        /* XXX - Add the POS reward to the block forger */
        long augmentedFeeNQT = RewardsImpl.augmentFee(this, totalFeeNQT);
      
        Account generatorAccount = Account.addOrGetAccount(getGeneratorId());
        generatorAccount.apply(getGeneratorPublicKey(), this.height);
        generatorAccount.addToBalanceAndUnconfirmedBalanceNQT(augmentedFeeNQT);
        generatorAccount.addToForgedBalanceNQT(augmentedFeeNQT);
        for (TransactionImpl transaction : getTransactions()) {
            try {
                transaction.apply();
            } catch (RuntimeException e) {
                Logger.logErrorMessage(e.toString(), e);
                throw new BlockchainProcessor.TransactionNotAcceptedException(e, transaction);
            }
        }
    }

    void setPrevious(BlockImpl block) {
        if (block != null) {
            if (block.getId() != getPreviousBlockId()) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous block id doesn't match");
            }
            this.height = block.getHeight() + 1;
            this.calculateBaseTarget(block);
        } else {
            this.height = 0;
        }
        short index = 0;
        for (TransactionImpl transaction : getTransactions()) {
            transaction.setBlock(this);
            transaction.setIndex(index++);
        }
    }

    private void calculateBaseTarget(BlockImpl previousBlock) {

        if ((this.getId() != Genesis.GENESIS_BLOCK_ID || previousBlockId != 0) && cumulativeDifficulty.equals(BigInteger.ZERO)) {
            long curBaseTarget = previousBlock.baseTarget;
            
            /* XXX - Replaced hardcoded 60 with Constants.SECONDS_BETWEEN_BLOCKS */
            long newBaseTarget = BigInteger.valueOf(curBaseTarget)
                    .multiply(BigInteger.valueOf(this.timestamp - previousBlock.timestamp))
                    .divide(BigInteger.valueOf(Constants.SECONDS_BETWEEN_BLOCKS)).longValue();
            if (newBaseTarget < 0 || newBaseTarget > Constants.MAX_BASE_TARGET) {
                newBaseTarget = Constants.MAX_BASE_TARGET;
            }
            if (newBaseTarget < curBaseTarget / 2) {
                newBaseTarget = curBaseTarget / 2;
            }
            if (newBaseTarget == 0) {
                newBaseTarget = 1;
            }
            long twofoldCurBaseTarget = curBaseTarget * 2;
            if (twofoldCurBaseTarget < 0) {
                twofoldCurBaseTarget = Constants.MAX_BASE_TARGET;
            }
            if (newBaseTarget > twofoldCurBaseTarget) {
                newBaseTarget = twofoldCurBaseTarget;
            }
            baseTarget = newBaseTarget;
            cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(Convert.two64.divide(BigInteger.valueOf(baseTarget)));
        }
    }

}
