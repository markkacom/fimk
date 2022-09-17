package nxt;

import java.util.List;

public class RewardsImpl {
  
    static public long augmentFee(Block block , long totalFeeNQT) {    
        long rewardNQT = calculatePOSRewardNQT(block);
        long totalRewardNQT = Math.addExact(rewardNQT, totalFeeNQT);

        processPOPRewarding(block.getHeight());

        return totalRewardNQT;
    }
    
    static public long calculatePOSRewardNQT(Block block) {
        return calculatePOSRewardNQT(block.getHeight());
    }
    
    static public long calculatePOSRewardNQT(int height) {
        if (height >= Constants.FORGER_FEE_BLOCK) {
            for (int i = 0; i < Constants.FORGER_FEE_AMOUNT_NQT_STAGES.length; i++) {
                if (height < (Constants.FORGER_FEE_STAGE_CHANGE_AT_BLOCK * (i + 1))) {
                    return Constants.FORGER_FEE_AMOUNT_NQT_STAGES[i];
                }
            }
        }
        return 0;
    }

    static public long processPOPRewarding(int height) {
        if (height < Constants.FORGER_FEE_BLOCK) return 0;
        List<AssetRewarding> ars = AssetRewarding.getApplicableRewardings(height);
        ars.forEach(assetRewarding -> System.out.println(assetRewarding));
        return 0;
    }
}