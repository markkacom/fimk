package nxt.reward;

import nxt.*;
import nxt.db.DbIterator;
import nxt.peer.rewarding.NodesMonitoringThread;
import nxt.txn.AssetRewardingTxnType.LotteryType;
import nxt.txn.AssetRewardingTxnType.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static nxt.reward.AssetRewarding.getApplicableRewardings;

/**
 * POP rewards for candidates formed on login actions.
 */
public class RewardingLogined extends Rewarding {

    @Override
    public void applyPOPRewards(Block block) {
        if (!HardFork.POS_POP_REWARD_BLOCK(block.getHeight())) return;

        RewardCandidate.removeObsolete(Nxt.getBlockchain().getHeight());

        Candidates candidates = getRewardCandidates();

        applyBlockPOPRewards(block, candidates);

        applyAssetPOPRewards(block, candidates);

        if (NodesMonitoringThread.roundSuccess) {
            // send rewards
        }
    }

    private void applyBlockPOPRewards(Block block, Candidates candidates) {
        RewardCandidate winner = resolveMoneyWinner(candidates, block);
        if (winner != null) {
            Account winnerAccount = Account.addOrGetAccount(winner.getAccount());
            // money inflation
            winnerAccount.addToBalanceAndUnconfirmedBalanceNQT(Constants.POP_REWARD_MONEY_AMOUNT_NQT);
            RewardItem.registerReward(new RewardItem(
                    block.getHeight(), -1, RewardItem.NAME.POP_REWARD_MONEY, winnerAccount.getId(),
                    0, Constants.POP_REWARD_MONEY_AMOUNT_NQT
            ));
        }
    }

    private void applyAssetPOPRewards(Block block, Candidates candidates) {
        List<RewardItem> rewards = resolveAssetRewards(block, candidates);
        //System.out.printf("block %d  POP rewards %d \n", block.getHeight(), rewards == null ? 0 : rewards.size());

        if (rewards != null) {
            for (RewardItem reward : rewards) {
                //System.out.println(reward.toString());
                Account winnerAccount = Account.addOrGetAccount(reward.accountId);
                // asset inflation
                winnerAccount.addToAssetAndUnconfirmedAssetBalanceQNT(reward.assetId, reward.amount);
                RewardItem.registerReward(reward);
            }
        }
    }

    /**
     * @return array of 1) account id, 2) private asset id, 3) amount
     */
    private List<RewardItem> resolveAssetRewards(Block block, Candidates candidates) {
        List<AssetRewarding> ars = getApplicableRewardings(block.getHeight());
        List<RewardItem> result = ars.isEmpty() ? null : new ArrayList<>(ars.size());
        for (AssetRewarding ar : ars) {
            Target target = Target.get(ar.getTarget());
            if (target == Target.FORGER) {
                if (MofoAsset.getAccountAllowed(ar.getAsset(), block.getGeneratorId())) {
                    long amount = halving(ar.getBaseAmount(), ar.getHalvingBlocks(), ar.getHeight(), block.getHeight());
                    if (amount > 0) {
                        result.add(new RewardItem(block.getHeight(), ar.getId(), RewardItem.NAME.FORGER, block.getGeneratorId(), ar.getAsset(), amount));
                    }
                }
            }
            if (target == Target.CONSTANT_ACCOUNT) {
                if (MofoAsset.getAccountAllowed(ar.getAsset(), ar.getTargetInfo())) {
                    long amount = halving(ar.getBaseAmount(), ar.getHalvingBlocks(), ar.getHeight(), block.getHeight());
                    if (amount > 0) {
                        result.add(new RewardItem(block.getHeight(), ar.getId(), RewardItem.NAME.CONSTANT_ACCOUNT, ar.getTargetInfo(), ar.getAsset(), amount));
                    }
                }
            }
            if (target == Target.REGISTERED_POP_REWARD_RECEIVER) {
                if (candidates.candidates.isEmpty()) continue;
                List<RewardCandidate> privateAssetCandidates = new ArrayList<>();
                for (RewardCandidate candidate : candidates.candidates) {
                    if (MofoAsset.getAccountAllowed(ar.getAsset(), candidate.getAccount())) {
                        privateAssetCandidates.add(candidate);
                    }
                }
                //System.out.println("actual candidates " + privateAssetCandidates.size());
                if (privateAssetCandidates.isEmpty()) continue;
                RewardCandidate winner = privateAssetCandidates.size() == 1 ? privateAssetCandidates.get(0) : null;
                long altAssetId = ar.getTargetInfo();   // altAssetId == 0 means fimk
                LotteryType lotteryType = LotteryType.get(ar.getLotteryType());
                if (lotteryType == LotteryType.RANDOM_ACCOUNT) {
                    if (winner == null) {
                        int selectedIndex = (int) mapToBounded(privateAssetCandidates.size(), block.getId());
                        winner = privateAssetCandidates.get(selectedIndex);
                    }
                    Account selectedAccount = Account.getAccount(winner.getAccount());
                    if (selectedAccount == null) continue;
                    winner.altBalance = altAssetId == 0
                            ? selectedAccount.getGuaranteedBalanceNQT()
                            : selectedAccount.getAssetBalanceQNT(altAssetId);
                    // reward = baseAmount * balance / balanceDivider
                    long rewardAmount = ar.getBaseAmount() * winner.altBalance / ar.getBalanceDivider();
                    // reward amount have min and max limits
                    rewardAmount = Math.max(rewardAmount, ar.getBaseAmount() / 10);
                    rewardAmount = Math.max(rewardAmount, 1);
                    rewardAmount = Math.min(rewardAmount, ar.getBaseAmount() * 10);
                    long amount = halving(rewardAmount, ar.getHalvingBlocks(), ar.getHeight(), block.getHeight());
                    if (amount > 0) {
                        result.add(new RewardItem(block.getHeight(), ar.getId(), RewardItem.NAME.RANDOM_ACCOUNT, winner.getAccount(), ar.getAsset(), amount));
                    }
                }
                if (lotteryType == LotteryType.RANDOM_WEIGHTED_ACCOUNT) {
                    if (winner == null) {
                        long accum = 0;
                        for (RewardCandidate candidate : privateAssetCandidates) {
                            if (altAssetId == 0) {
                                Account account = Account.getAccount(candidate.getAccount());
                                if (account == null) continue;
                                candidate.altBalance = account.getBalanceNQT();
                            } else {
                                candidate.altBalance = Account.getAssetBalanceQNT(candidate.getAccount(), altAssetId);
                            }
                            accum += Math.max(candidate.altBalance, ar.getBaseAmount());
                        }
                        long v = mapToBounded(accum, block.getId());
                        accum = 0;
                        for (RewardCandidate candidate : privateAssetCandidates) {
                            accum += Math.max(candidate.altBalance, ar.getBaseAmount());
                            if (accum > v) {
                                winner = candidate;
                                break;
                            }
                        }
                    }
                    if (winner != null) {
                        long amount = halving(ar.getBaseAmount(), ar.getHalvingBlocks(), ar.getHeight(), block.getHeight());
                        if (amount > 0) {
                            result.add(new RewardItem(block.getHeight(), ar.getId(), RewardItem.NAME.RANDOM_WEIGHTED_ACCOUNT, winner.getAccount(), ar.getAsset(), amount));
                        }
                    }
                }
            }
        }
        return result;
    }

    private RewardCandidate resolveMoneyWinner(Candidates candidates, Block block) {
        if (candidates.candidates.isEmpty()) return null;
        long threshold = mapToBounded(candidates.balanceTotal, block.getId());
        long accum = 0;
        for (RewardCandidate candidate : candidates.candidates) {
            accum += candidate.getBalanceLimitedBottom();
            if (accum > threshold) {
                return candidate;  // winner
            }
        }
        return null;
    }

    Candidates getRewardCandidates() {
        DbIterator<RewardCandidate> it = RewardCandidate.getActualCandidates(
                Nxt.getBlockchain().getHeight() - Constants.REWARD_APPLICANT_REGISTRATION_EXPIRY_LIMIT);
        List<RewardCandidate> candidates = new ArrayList<>();
        AtomicLong balanceTotal = new AtomicLong();
        it.forEach(candidate -> {
            Account account = Account.getAccount(candidate.getAccount());
            if (account != null) {
                candidate.balance = account.getBalanceNQT();
                candidates.add(candidate);
                balanceTotal.set(balanceTotal.get() + candidate.getBalanceLimitedBottom());
            }
        });
        return new Candidates(candidates, balanceTotal.get());
    }

    private long halving(long amount, int everyNum, int fromHeight, int height) {
        if (everyNum == 0) return amount;
        int n = (height - fromHeight) / everyNum;
        // Math.pow(2, 1024) is infinitive
        if (n > 1023) return 0;
        return amount / (long) Math.pow(2, n);
    }

    /**
     * Translate passed source value from range [0..Long.MAX_VALUE] to range [0..bound]
     */
    private long mapToBounded(long bound, long source) {
        long coef = Long.MAX_VALUE / bound;
        // when bound is very close to Long.MAX_VALUE the precision limit is not enough (instead of 3.99999... we get 4.0)
        // So we should force result decrease by 1 in such case because the bound is exclusive
        return Math.min(Math.abs(source) / coef, bound - 1);
    }

    private static class Candidates {
        public Candidates(List<RewardCandidate> candidates, long balanceTotal) {
            this.candidates = candidates;
            this.balanceTotal = balanceTotal;
        }

        List<RewardCandidate> candidates;
        long balanceTotal;
    }

}