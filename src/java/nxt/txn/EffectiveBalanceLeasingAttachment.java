package nxt.txn;

import nxt.Attachment;
import nxt.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public final class EffectiveBalanceLeasingAttachment extends Attachment.AbstractAttachment {

    private final short period;

    public EffectiveBalanceLeasingAttachment(ByteBuffer buffer, byte transactionVersion) {
        super(buffer, transactionVersion);
        this.period = buffer.getShort();
    }

    public EffectiveBalanceLeasingAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.period = ((Long) attachmentData.get("period")).shortValue();
    }

    public EffectiveBalanceLeasingAttachment(short period) {
        this.period = period;
    }

    @Override
    protected int getMySize() {
        return 2;
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        buffer.putShort(period);
    }

    @Override
    protected void putMyJSON(JSONObject attachment) {
        attachment.put("period", period);
    }

    @Override
    public TransactionType getTransactionType() {
        return AccountControlTxnType.EFFECTIVE_BALANCE_LEASING;
    }

    public short getPeriod() {
        return period;
    }
}
