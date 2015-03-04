package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.MofoAsset;
import nxt.MofoAttachment;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.FEATURE_NOT_AVAILABLE;
import static nxt.http.JSONResponses.INCORRECT_ASSET;

public final class AddPrivateAssetAccount extends CreateTransaction {

    static final AddPrivateAssetAccount instance = new AddPrivateAssetAccount();

    private AddPrivateAssetAccount() {
        super(new APITag[] {APITag.AE, APITag.MOFO}, "asset");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        if ( ! Asset.privateEnabled()) {
            return FEATURE_NOT_AVAILABLE;
        }      
        long recipientId = ParameterParser.getRecipientId(req);
        Asset asset = ParameterParser.getAsset(req);
        if ( ! MofoAsset.isPrivateAsset(asset)) {
            return INCORRECT_ASSET;
        }

        Account senderAccount = ParameterParser.getSenderAccount(req);
        Attachment attachment = new MofoAttachment.AddPrivateAssetAccountAttachment(asset.getId());
        return createTransaction(req, senderAccount, recipientId, 0, attachment);
    }
}