package nxt.reward;

import nxt.*;
import nxt.db.DbIterator;
import nxt.peer.rewarding.NodesMonitoringThread;
import nxt.txn.AssetRewardingTxnType.LotteryType;
import nxt.txn.AssetRewardingTxnType.Target;

import java.util.ArrayList;
import java.util.List;

public class RewardImpl extends Reward {

    public long augmentFee(Block block, long totalFeeNQT) {
        long rewardNQT = calculatePOSRewardNQT(block);
        long totalRewardNQT = Math.addExact(rewardNQT, totalFeeNQT);

        if (NodesMonitoringThread.roundSuccess) {
            // send rewards
        }

        List<AssetRewarding.AssetReward> rewards = processPOPRewarding(block);
        System.out.printf("block %d  pop rewards %d \n", block.getHeight(), rewards == null ? 0 : rewards.size());
        if (rewards != null) {
            rewards.forEach(assetReward -> System.out.println(assetReward.toString()));
        }

        return totalRewardNQT;
    }

    public long calculatePOSRewardNQT(Block block) {
        return calculatePOSRewardNQT(block.getHeight());
    }

    public long calculatePOSRewardNQT(int height) {
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
     * @return array of 1) account id, 2) private asset id, 3) amount
     */
    private List<AssetRewarding.AssetReward> processPOPRewarding(Block block) {
        if (!HardFork.PRIVATE_ASSETS_REWARD_BLOCK(block.getHeight())) return null;

        RewardCandidate.removeExpired(Nxt.getBlockchain().getHeight());

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
                List<RewardCandidate> candidates = rewardCandidates();
                if (candidates.isEmpty()) continue;
                long altAssetId = ar.getTargetInfo();   // altAssetId == 0 means fimk
                LotteryType lotteryType = LotteryType.get(ar.getLotteryType());
                if (lotteryType == LotteryType.RANDOM_ACCOUNT) {
                    int selectedIndex = (int) mapToBounded(candidates.size(), block.getId());
                    RewardCandidate selected = candidates.get(selectedIndex);
                    Account selectedAccount = Account.getAccount(selected.getAccount());
                    if (selectedAccount == null) continue;
                    selected.balance = altAssetId == 0
                            ? selectedAccount.getGuaranteedBalanceNQT()
                            : selectedAccount.getAssetBalanceQNT(altAssetId);
                    // reward = baseAmount * balance / balanceDivider
                    long rewardAmount = ar.getBaseAmount() * selected.balance / ar.getBalanceDivider();
                    // reward amount have min and max limits
                    rewardAmount = Math.max(rewardAmount, ar.getBaseAmount() / 10);
                    rewardAmount = Math.min(rewardAmount, ar.getBaseAmount() * 10);
                    result.add(new AssetRewarding.AssetReward(selected.getAccount(), ar.getAsset(), rewardAmount));
                }
                if (lotteryType == LotteryType.RANDOM_WEIGHTED_ACCOUNT) {
                    long accum = 0;
                    for (RewardCandidate candidate : candidates) {
                        Account account = Account.getAccount(candidate.getAccount());
                        if (account == null) continue;
                        candidate.balance = altAssetId == 0
                                ? account.getGuaranteedBalanceNQT()
                                : account.getAssetBalanceQNT(altAssetId);
                        accum += Math.max(candidate.balance, ar.getBaseAmount());
                    }
                    long v = mapToBounded(accum, block.getId());
                    accum = 0;
                    for (RewardCandidate candidate : candidates) {
                        accum += Math.max(candidate.balance, ar.getBaseAmount());
                        if (accum > v) {
                            result.add(new AssetRewarding.AssetReward(candidate.getAccount(), ar.getAsset(), ar.getBaseAmount()));
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private List<RewardCandidate> rewardCandidates() {
        DbIterator<RewardCandidate> it = RewardCandidate.getActualCandidates(
                Nxt.getBlockchain().getHeight() - Constants.REWARD_APPLICANT_REGISTRATION_EXPIRY_LIMIT);
        List<RewardCandidate> candidates = new ArrayList<>();
        it.forEach(candidates::add);
        return candidates;
    }

    /**
     * Translate passed source value from range [0..Long.MAX_VALUE] to range [0..bound]
     */
    private long mapToBounded(long bound, long source) {
        long coef = Long.MAX_VALUE / bound;
        return Math.abs(source) / coef;
    }

}