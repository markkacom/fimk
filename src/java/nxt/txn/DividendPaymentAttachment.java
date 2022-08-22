package nxt.txn;

import nxt.Attachment;
import nxt.TransactionType;
import nxt.util.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public final class DividendPaymentAttachment extends Attachment.AbstractAttachment {

    private final long assetId;
    private final int height;
    private final long amountNQTPerQNT;

    DividendPaymentAttachment(ByteBuffer buffer, byte transactionVersion) {
        super(buffer, transactionVersion);
        this.assetId = buffer.getLong();
        this.height = buffer.getInt();
        this.amountNQTPerQNT = buffer.getLong();
    }

    DividendPaymentAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
        this.height = ((Long) attachmentData.get("height")).intValue();
        this.amountNQTPerQNT = Convert.parseLong(attachmentData.get("amountNQTPerQNT"));
    }

    public DividendPaymentAttachment(long assetId, int height, long amountNQTPerQNT) {
        this.assetId = assetId;
        this.height = height;
        this.amountNQTPerQNT = amountNQTPerQNT;
    }

    @Override
    protected int getMySize() {
        return 8 + 4 + 8;
    }

    @Override
    protected void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(assetId);
        buffer.putInt(height);
        buffer.putLong(amountNQTPerQNT);
    }

    @Override
    protected void putMyJSON(JSONObject attachment) {
        attachment.put("asset", Long.toUnsignedString(assetId));
        attachment.put("height", height);
        attachment.put("amountNQTPerQNT", amountNQTPerQNT);
    }

    @Override
    public TransactionType getTransactionType() {
        return ColoredCoinsTxnTypes.DIVIDEND_PAYMENT;
    }

    public long getAssetId() {
        return assetId;
    }

    public int getHeight() {
        return height;
    }

    public long getAmountNQTPerQNT() {
        return amountNQTPerQNT;
    }

}
