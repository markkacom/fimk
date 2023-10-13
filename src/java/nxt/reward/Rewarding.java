package nxt.reward;

import nxt.Block;
import nxt.Constants;
import nxt.HardFork;

public abstract class Rewarding {

    private static final class InstanceHolder {
        static final Rewarding instance = new RewardingLogined();
    }

    public static Rewarding get() {
        return InstanceHolder.instance;
    }

    public long augmentFee(int height, long totalFeeNQT, long accountId) {
        long rewardNQT = calculatePOSRewardNQT(height);
        if (rewardNQT > 0) {
            RewardItem.registerReward(new RewardItem(height, 0, RewardItem.NAME.POS_REWARD, accountId, 0, rewardNQT));
            return Math.addExact(rewardNQT, totalFeeNQT);
        }
        return totalFeeNQT;
    }

    public abstract void applyPOPRewards(Block block);

    public long calculatePOSRewardNQT(int height) {
        if (height >= Constants.FORGER_FEE_BLOCK) {
            for (int i = 0; i < Constants.FORGER_FEE_AMOUNT_NQT_STAGES.length; i++) {
                if (height < (Constants.FORGER_FEE_STAGE_CHANGE_AT_BLOCK * (i + 1))) {
                    return Constants.FORGER_FEE_AMOUNT_NQT_STAGES[i];
                }
            }
        }

        if (HardFork.POS_POP_REWARD_BLOCK(height)) {
            return Constants.ONE_NXT;
        }

        return 0;
    }

}
