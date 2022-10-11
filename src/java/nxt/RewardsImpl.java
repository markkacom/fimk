package nxt;

import nxt.db.DbIterator;
import nxt.txn.AssetRewardingTxnType.LotteryType;
import nxt.txn.AssetRewardingTxnType.Target;

import java.util.ArrayList;
import java.util.List;

public class RewardsImpl {

    public static long augmentFee(Block block , long totalFeeNQT) {
        long rewardNQT = calculatePOSRewardNQT(block);
        long totalRewardNQT = Math.addExact(rewardNQT, totalFeeNQT);

        processPOPRewarding(block);

        return totalRewardNQT;
    }

    public static long calculatePOSRewardNQT(Block block) {
        return calculatePOSRewardNQT(block.getHeight());
    }

    public static long calculatePOSRewardNQT(int height) {
        if (height >= Constants.FORGER_FEE_BLOCK) {
            for (int i = 0; i < Constants.FORGER_FEE_AMOUNT_NQT_STAGES.length; i++) {
                if (height < (Constants.FORGER_FEE_STAGE_CHANGE_AT_BLOCK * (i + 1))) {
                    return Constants.FORGER_FEE_AMOUNT_NQT_STAGES[i];
                }
            }
        }
        return 0;
    }

    /**
     *
     * @return array of 1) account id, 2) private asset id, 3) amount
     */
    public static List<AssetRewarding.AssetReward> processPOPRewarding(Block block) {
        if (HardFork.PRIVATE_ASSETS_REWARD_BLOCK(block.getHeight())) return null;
        List<AssetRewarding> ars = AssetRewarding.getApplicableRewardings(block.getHeight());
        List<AssetRewarding.AssetReward> result = ars.isEmpty() ? null : new ArrayList<>(ars.size());
        for (AssetRewarding ar : ars) {
            Target target = Target.get(ar.getTarget());
            if (target == Target.FORGER) {
                result.add(new AssetRewarding.AssetReward(block.getGeneratorId(), ar.getAsset(), ar.getBaseAmount()));
            }
            if (target == Target.CONSTANT_ACCOUNT) {
                result.add(new AssetRewarding.AssetReward(ar.getTargetInfo(), ar.getAsset(), ar.getBaseAmount()));
            }
            if (target == Target.REGISTERED_POP_REWARD_RECEIVER) {
                //todo filter expired candidate
                DbIterator<RewardCandidate> it = RewardCandidate.getRewardCandidatesSorted(
                        ar.getAsset(), 0, Integer.MAX_VALUE);
                List<RewardCandidate> candidates = new ArrayList<>();
                it.forEachRemaining(candidates::add);
                if (candidates.isEmpty()) continue;
                int selectedIndex = AssetRewarding.mapToBounded(candidates.size(), block.getId());
                RewardCandidate selected = candidates.get(selectedIndex);
                LotteryType lotteryType = LotteryType.get(ar.getLotteryType());
                long altAssetId = ar.getTargetInfo();   // altAssetId == 0 means fimk
                Account selectedAccount = Account.getAccount(selected.getId());
                long balance = altAssetId == 0
                        ? selectedAccount.getGuaranteedBalanceNQT()
                        : selectedAccount.getAssetBalanceQNT(altAssetId);
                if (lotteryType == LotteryType.RANDOM_ACCOUNT) {
                    // reward = baseAmount * balance / balanceDivider
                    long rewardAmount = ar.getBaseAmount() * balance / ar.getBalanceDivider();
                    result.add(new AssetRewarding.AssetReward(block.getGeneratorId(), ar.getAsset(), rewardAmount));
                }
                if (lotteryType == LotteryType.RANDOM_WEIGHTED_ACCOUNT) {

                }
            }
        }
        return result;
    }
}