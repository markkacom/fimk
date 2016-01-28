package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.MofoAsset;
import nxt.MofoAttachment;
import nxt.NxtException;

import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ASSET;

public final class RemovePrivateAssetAccount extends CreateTransaction {

    static final RemovePrivateAssetAccount instance = new RemovePrivateAssetAccount();

    private RemovePrivateAssetAccount() {
        super(new APITag[] {APITag.AE, APITag.MOFO}, "asset");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

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