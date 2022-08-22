package nxt.txn;

import nxt.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public final class AskOrderCancellationAttachment extends OrderCancellationAttachment {

    AskOrderCancellationAttachment(ByteBuffer buffer, byte transactionVersion) {
        super(buffer, transactionVersion);
    }

    AskOrderCancellationAttachment(JSONObject attachmentData) {
        super(attachmentData);
    }

    public AskOrderCancellationAttachment(long orderId) {
        super(orderId);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoinsTxnTypes.ASK_ORDER_CANCELLATION;
    }

}
