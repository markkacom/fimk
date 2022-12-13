package nxt.txn;

import nxt.Constants;
import nxt.TransactionType;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public final class BidOrderPlacementAttachment extends OrderPlacementAttachment {

    private final int timestamp;
    private final long orderFeeNQT;

    BidOrderPlacementAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) {
        super(buffer, transactionVersion);
        this.timestamp = timestamp;
        this.orderFeeNQT = timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP ? buffer.getLong() : 0;
    }

    BidOrderPlacementAttachment(JSONObject attachmentData, int timestamp) {
        super(attachmentData);
        this.timestamp = timestamp;
        this.orderFeeNQT = timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP ? Convert.parseLong(attachmentData.get("orderFeeNQT")) : 0;
    }

    public BidOrderPlacementAttachment(long assetId, long quantityQNT, long priceNQT, long orderFeeNQT) {
        super(assetId, quantityQNT, priceNQT);
        this.timestamp = Integer.MAX_VALUE;
        this.orderFeeNQT = orderFeeNQT;
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoinsTxnTypes.BID_ORDER_PLACEMENT;
    }

    @Override
    protected int getMySize() {
        return super.getMySize() + (timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP ? 8 : 0);
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        super.putMyBytes(buffer);
        if (timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP) {
            buffer.putLong(orderFeeNQT);
        }
    }

    @Override
    protected void putMyJSON(JSONObject attachment) {
        super.putMyJSON(attachment);
        if (timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP) {
            attachment.put("orderFeeNQT", orderFeeNQT);
        }
    }

    public long getOrderFeeNQT() {
        return orderFeeNQT;
    }

}
