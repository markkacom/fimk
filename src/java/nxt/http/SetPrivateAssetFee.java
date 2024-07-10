package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.*;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=setPrivateAssetFee")
public final class SetPrivateAssetFee extends CreateTransaction {

    static final SetPrivateAssetFee instance = new SetPrivateAssetFee();

    private SetPrivateAssetFee() {
        super(new APITag[] {APITag.AE, APITag.MOFO}, "asset", "orderFeePercentage", "tradeFeePercentage");
    }

    @Override
    @Operation(summary = "Set private asset fee",
            tags = {APITag2.AE, APITag2.MOFO})
    @Parameter(name = "asset", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "asset id")
    @Parameter(name = "orderFeePercentage", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "order fee percentage")
    @Parameter(name = "tradeFeePercentage", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "trade fee percentage")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

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