package nxt.txn;

import nxt.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public final class BidOrderCancellationAttachment extends OrderCancellationAttachment {

    BidOrderCancellationAttachment(ByteBuffer buffer, byte transactionVersion) {
        super(buffer, transactionVersion);
    }

    BidOrderCancellationAttachment(JSONObject attachmentData) {
        super(attachmentData);
    }

    public BidOrderCancellationAttachment(long orderId) {
        super(orderId);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoinsTxnTypes.BID_ORDER_CANCELLATION;
    }

}
