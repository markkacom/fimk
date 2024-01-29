package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nxt.*;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.INCORRECT_ASSET;

@Path("/fimk?requestType=addPrivateAssetAccount")
public final class AddPrivateAssetAccount extends CreateTransaction {

    static final AddPrivateAssetAccount instance = new AddPrivateAssetAccount();

    private AddPrivateAssetAccount() {
        super(new APITag[] {APITag.AE, APITag.MOFO}, "asset");
    }

    @Override
    @POST
    @Operation(summary = "Enable private asset for account",
            tags = {APITag2.ACCOUNT})
    @Parameter(name = "asset", in = ParameterIn.QUERY, required = true)
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

        long recipientId = ParameterParser.getAccountId(req, "recipient", true);
        Asset asset = ParameterParser.getAsset(req);
        if ( ! MofoAsset.isPrivateAsset(asset)) {
            return INCORRECT_ASSET;
        }

        Account senderAccount = ParameterParser.getSenderAccount(req);
        Attachment attachment = new MofoAttachment.AddPrivateAssetAccountAttachment(asset.getId());
        return createTransaction(req, senderAccount, recipientId, 0, attachment);
    }
}