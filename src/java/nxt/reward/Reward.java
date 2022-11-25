package nxt.reward;

import nxt.Block;

public abstract class Reward {

    public abstract void applyPOPReward(Block block);

    private static final class InstanceHolder {
        static final Reward instance = new RewardImpl();
    }

    public static Reward get() {
        return InstanceHolder.instance;
    }

    public abstract long augmentFee(Block block, long totalFeeNQT);

    public abstract long calculatePOSRewardNQT(Block block);

    public abstract long calculatePOSRewardNQT(int height);

}
