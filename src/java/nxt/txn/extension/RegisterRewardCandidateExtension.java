package nxt.txn.extension;

import nxt.*;
import nxt.reward.RewardCandidate;
import nxt.util.Logger;

/**
 * @deprecated instead of this the login registration transaction is used
 */
class RegisterRewardCandidateExtension extends NamespacedAliasBasedExtension {

    // "(FTR.n.v)"  first number n to distinguish features (extensions), second number v to distinguish versions of the feature
    private static final String MARK = "(FTR.1.0)";

    @Override
    protected String getMark() {
        return MARK;
    }

    @Override
    public String getName() {
        return "Register Reward Candidate";
    }

    public String process(boolean validateOnly, Transaction transaction, Account sender, Account recipient) {
        String validateResult = validate(transaction);
        if (validateResult != null) return validateResult;

        MofoAttachment.NamespacedAliasAssignmentAttachment a =
                (MofoAttachment.NamespacedAliasAssignmentAttachment) transaction.getAttachment();

        // payload format "assetId",
        // examples "834538499053643"
        String payload = a.getAliasURI();
        if (payload.trim().isEmpty()) return "Wrong payload";

        long assetId;
        try {
            assetId = Long.parseUnsignedLong(payload.trim());
        } catch (NumberFormatException e) {
            String resultMessage = "Transaction payload is wrong";
            Logger.logErrorMessage(resultMessage, e);
            return resultMessage;
        }

        Asset asset;
        if (assetId != 0) {
            asset = Asset.getAsset(assetId);
            if (asset == null) return "Asset is not found";
            //if (asset.getAccountId() != sender.getId()) return "Sender is not the issuer of the asset";
            if (!MofoAsset.isPrivateAsset(asset)) return "Asset is not private";
        }

        if (validateOnly) return null;

        //apply

        RewardCandidate.save(transaction, assetId);
        return null;  //successful outcome
    }
}
