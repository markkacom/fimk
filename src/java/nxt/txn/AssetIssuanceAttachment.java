package nxt.txn;

import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.TransactionType;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public final class AssetIssuanceAttachment extends Attachment.AbstractAttachment {

    private final int timestamp;
    private final String name;
    private final String description;
    private final long quantityQNT;
    private final byte decimals;
    private final byte type;

    AssetIssuanceAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) throws NxtException.NotValidException {
        super(buffer, transactionVersion);
        this.timestamp = timestamp;
        this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ASSET_NAME_LENGTH);
        this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_DESCRIPTION_LENGTH);
        this.quantityQNT = buffer.getLong();
        this.decimals = buffer.get();
        this.type = timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP ? buffer.get() : 0;
    }

    AssetIssuanceAttachment(JSONObject attachmentData, int timestamp) {
        super(attachmentData);
        this.timestamp = timestamp;
        this.name = (String) attachmentData.get("name");
        this.description = Convert.nullToEmpty((String) attachmentData.get("description"));
        this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
        this.decimals = ((Long) attachmentData.get("decimals")).byteValue();
        this.type = timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP ? ((Long) attachmentData.get("type")).byteValue() : 0;
    }

    public AssetIssuanceAttachment(String name, String description, long quantityQNT, byte decimals, byte type) {
        this.timestamp = Integer.MAX_VALUE;
        this.name = name;
        this.description = Convert.nullToEmpty(description);
        this.quantityQNT = quantityQNT;
        this.decimals = decimals;
        this.type = type;
    }

    @Override
    protected int getMySize() {
        int size = 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 8 + 1;
        if (timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP) {
            size += 1;
        }
        ;
        return size;
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        byte[] name = Convert.toBytes(this.name);
        byte[] description = Convert.toBytes(this.description);
        buffer.put((byte) name.length);
        buffer.put(name);
        buffer.putShort((short) description.length);
        buffer.put(description);
        buffer.putLong(quantityQNT);
        buffer.put(decimals);
        if (timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP) {
            buffer.put(type);
        }
    }

    @Override
    protected void putMyJSON(JSONObject attachment) {
        attachment.put("name", name);
        attachment.put("description", description);
        attachment.put("quantityQNT", quantityQNT);
        attachment.put("decimals", decimals);
        if (timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP) {
            attachment.put("type", type);
        }
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoinsTxnTypes.ASSET_ISSUANCE;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getQuantityQNT() {
        return quantityQNT;
    }

    public byte getDecimals() {
        return decimals;
    }

    public byte getType() {
        return type;
    }
}
