package nxt.http;

import io.swagger.v3.oas.annotations.Parameter;
import nxt.*;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;

import static nxt.http.JSONResponses.INCORRECT_ASSET;

//@Path("/fimk?requestType=accountColorList")
public final class RemovePrivateAssetAccount extends CreateTransaction {

    static final RemovePrivateAssetAccount instance = new RemovePrivateAssetAccount();

    private RemovePrivateAssetAccount() {
        super(new APITag[] {APITag.AE, APITag.MOFO}, "asset");
    }

    @Override
    @POST
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long recipientId = ParameterParser.getAccountId(req, "recipient", true);
        Asset asset = ParameterParser.getAsset(req);
        if ( ! MofoAsset.isPrivateAsset(asset)) {
            return INCORRECT_ASSET;
        }

        Account senderAccount = ParameterParser.getSenderAccount(req);
        Attachment attachment = new MofoAttachment.RemovePrivateAssetAccountAttachment(asset.getId());
        return createTransaction(req, senderAccount, recipientId, 0, attachment);
    }
}