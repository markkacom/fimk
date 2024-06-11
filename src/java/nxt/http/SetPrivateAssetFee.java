package nxt.http;

import io.swagger.v3.oas.annotations.Parameter;
import nxt.*;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;


////@Path("/fimk?requestType=accountColorList")
public final class
SetPrivateAssetFee extends CreateTransaction {

    static final SetPrivateAssetFee instance = new SetPrivateAssetFee();

    private SetPrivateAssetFee() {
        super(new APITag[] {APITag.AE, APITag.MOFO}, "asset", "orderFeePercentage", "tradeFeePercentage");
    }

    @Override
    @POST
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

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