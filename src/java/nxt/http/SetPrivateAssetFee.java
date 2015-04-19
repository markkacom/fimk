package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.Constants;
import nxt.MofoAttachment;
import nxt.NxtException;

import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.FEATURE_NOT_AVAILABLE;

public final class SetPrivateAssetFee extends CreateTransaction {

    static final SetPrivateAssetFee instance = new SetPrivateAssetFee();

    private SetPrivateAssetFee() {
        super(new APITag[] {APITag.AE, APITag.MOFO}, "asset", "orderFeePercentage", "tradeFeePercentage");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        if ( ! Asset.privateEnabled()) {
            return FEATURE_NOT_AVAILABLE;
        }      
        Asset asset = ParameterParser.getAsset(req);
        int orderFeePercentage = ParameterParser.getInt(req, "orderFeePercentage", 
            Constants.MIN_PRIVATE_ASSET_FEE_PERCENTAGE, Constants.MAX_PRIVATE_ASSET_FEE_PERCENTAGE, true);
        int tradeFeePercentage = ParameterParser.getInt(req, "tradeFeePercentage", 
            Constants.MIN_PRIVATE_ASSET_FEE_PERCENTAGE, Constants.MAX_PRIVATE_ASSET_FEE_PERCENTAGE, true);

        Account senderAccount = ParameterParser.getSenderAccount(req);
        Attachment attachment = new MofoAttachment.PrivateAssetSetFeeAttachment(asset.getId(), orderFeePercentage, tradeFeePercentage);
        return createTransaction(req, senderAccount, attachment);
    }
}