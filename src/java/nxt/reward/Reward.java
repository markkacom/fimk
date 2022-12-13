package nxt.reward;

import nxt.Block;

public abstract class Reward {

    private static final class InstanceHolder {

        static final Reward instance = new RewardImpl();
    }
    public static Reward get() {
        return InstanceHolder.instance;
    }

    public abstract long augmentFee(int height, long totalFeeNQT, long accountId);

    public abstract long calculatePOSRewardNQT(int height);

    public abstract void applyPOPReward(Block block);

}
