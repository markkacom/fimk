package nxt.txn;

import nxt.Constants;
import nxt.TransactionType;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public final class AskOrderPlacementAttachment extends OrderPlacementAttachment {

    private final long orderFeeQNT;
    private final int timestamp;

    AskOrderPlacementAttachment(ByteBuffer buffer, byte transactionVersion, int timestamp) {
        super(buffer, transactionVersion);
        this.timestamp = timestamp;
        this.orderFeeQNT = timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP ? buffer.getLong() : 0;
    }

    AskOrderPlacementAttachment(JSONObject attachmentData, int timestamp) {
        super(attachmentData);
        this.timestamp = timestamp;
        this.orderFeeQNT = timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP ? Convert.parseLong(attachmentData.get("orderFeeQNT")) : 0;
    }

    public AskOrderPlacementAttachment(long assetId, long quantityQNT, long priceNQT, long orderFeeQNT) {
        super(assetId, quantityQNT, priceNQT);
        this.timestamp = Integer.MAX_VALUE;
        this.orderFeeQNT = orderFeeQNT;
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoinsTxnTypes.ASK_ORDER_PLACEMENT;
    }

    @Override
    protected int getMySize() {
        return super.getMySize() + (timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP ? 8 : 0);
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        super.putMyBytes(buffer);
        if (timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP) {
            buffer.putLong(orderFeeQNT);
        }
    }

    @Override
    protected void putMyJSON(JSONObject attachment) {
        super.putMyJSON(attachment);
        if (timestamp > Constants.PRIVATE_ASSETS_TIMESTAMP) {
            attachment.put("orderFeeQNT", orderFeeQNT);
        }
    }

    public long getOrderFeeQNT() {
        return orderFeeQNT;
    }
}
