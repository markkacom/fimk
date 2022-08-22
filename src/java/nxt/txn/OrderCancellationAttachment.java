package nxt.txn;

import nxt.Attachment;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public abstract class OrderCancellationAttachment extends Attachment.AbstractAttachment {

    private final long orderId;

    OrderCancellationAttachment(ByteBuffer buffer, byte transactionVersion) {
        super(buffer, transactionVersion);
        this.orderId = buffer.getLong();
    }

    OrderCancellationAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.orderId = Convert.parseUnsignedLong((String) attachmentData.get("order"));
    }

    OrderCancellationAttachment(long orderId) {
        this.orderId = orderId;
    }

    @Override
    protected int getMySize() {
        return 8;
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(orderId);
    }

    @Override
    protected void putMyJSON(JSONObject attachment) {
        attachment.put("order", Long.toUnsignedString(orderId));
    }

    public long getOrderId() {
        return orderId;
    }
}
