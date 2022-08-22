package nxt.txn;

import nxt.*;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public final class AssetTransferAttachment extends Attachment.AbstractAttachment {

    private final long assetId;
    private final long quantityQNT;
    private final String comment;

    AssetTransferAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
        super(buffer, transactionVersion);
        this.assetId = buffer.getLong();
        this.quantityQNT = buffer.getLong();
        this.comment = getVersion() == 0 ? Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH) : null;
    }

    AssetTransferAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
        this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
        this.comment = getVersion() == 0 ? Convert.nullToEmpty((String) attachmentData.get("comment")) : null;
    }

    public AssetTransferAttachment(long assetId, long quantityQNT) {
        this.assetId = assetId;
        this.quantityQNT = quantityQNT;
        this.comment = null;
    }

    @Override
    protected int getMySize() {
        return 8 + 8 + (getVersion() == 0 ? (2 + Convert.toBytes(comment).length) : 0);
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(assetId);
        buffer.putLong(quantityQNT);
        if (getVersion() == 0 && comment != null) {
            byte[] commentBytes = Convert.toBytes(this.comment);
            buffer.putShort((short) commentBytes.length);
            buffer.put(commentBytes);
        }
    }

    @Override
    protected void putMyJSON(JSONObject attachment) {
        Asset.putAsset(attachment, assetId);
        attachment.put("quantityQNT", quantityQNT);
        if (getVersion() == 0) {
            attachment.put("comment", comment);
        }
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoinsTxnTypes.ASSET_TRANSFER;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getQuantityQNT() {
        return quantityQNT;
    }

    public String getComment() {
        return comment;
    }

}
