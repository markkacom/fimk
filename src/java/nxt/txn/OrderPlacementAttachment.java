package nxt.txn;

import nxt.Asset;
import nxt.Attachment;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public abstract class OrderPlacementAttachment extends Attachment.AbstractAttachment {

    private final long assetId;
    private final long quantityQNT;
    private final long priceNQT;

    OrderPlacementAttachment(ByteBuffer buffer, byte transactionVersion) {
        super(buffer, transactionVersion);
        this.assetId = buffer.getLong();
        this.quantityQNT = buffer.getLong();
        this.priceNQT = buffer.getLong();
    }

    OrderPlacementAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
        this.quantityQNT = Convert.parseLong(attachmentData.get("quantityQNT"));
        this.priceNQT = Convert.parseLong(attachmentData.get("priceNQT"));
    }

    OrderPlacementAttachment(long assetId, long quantityQNT, long priceNQT) {
        this.assetId = assetId;
        this.quantityQNT = quantityQNT;
        this.priceNQT = priceNQT;
    }

    @Override
    protected int getMySize() {
        return 8 + 8 + 8;
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(assetId);
        buffer.putLong(quantityQNT);
        buffer.putLong(priceNQT);
    }

    @Override
    protected void putMyJSON(JSONObject attachment) {
        Asset.putAsset(attachment, assetId);
        attachment.put("quantityQNT", quantityQNT);
        attachment.put("priceNQT", priceNQT);
    }

    public long getAssetId() {
        return assetId;
    }

    public long getQuantityQNT() {
        return quantityQNT;
    }

    public long getPriceNQT() {
        return priceNQT;
    }
}
