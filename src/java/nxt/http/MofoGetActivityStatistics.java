package nxt.http;

import nxt.MofoQueries;
import nxt.Nxt;
import nxt.NxtException;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class MofoGetActivityStatistics extends APIServlet.APIRequestHandler {

    static final MofoGetActivityStatistics instance = new MofoGetActivityStatistics();
    static final int SECONDS_24H = 24 * 60 * 60;
    static final int SECONDS_WEEK = 7 * SECONDS_24H;
    static final int SECONDS_MONTH = 30 * SECONDS_24H;
    
    private MofoGetActivityStatistics() {
        super(new APITag[] {APITag.MOFO} );
    }
    
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        JSONObject response = new JSONObject();        
        int time = Nxt.getBlockchain().getLastBlock().getTimestamp();
      
        response.put("averageBlockTime24H", MofoQueries.getAverageBlockTime24H());
        response.put("transactionCountToday", MofoQueries.getTransactionCountSince(time - SECONDS_24H));
        response.put("transactionCountWeek", MofoQueries.getTransactionCountSince(time - SECONDS_WEEK));
        response.put("transactionCountMonth", MofoQueries.getTransactionCountSince(time - SECONDS_MONTH));
        
        // TODO SQL statements are too slow currently.
        long dayReward = MofoQueries.getBlockRewardsSince(time - SECONDS_24H).getTotalRewardsNQT();
        // long weekReward = MofoQueries.getBlockRewardsSince(time - SECONDS_WEEK).getTotalRewards();
        // long monthReward = MofoQueries.getBlockRewardsSince(time - SECONDS_MONTH).getTotalRewards();
        long weekReward = 7 * dayReward;
        long monthReward = 30 * weekReward;

        response.put("rewardsToday", String.valueOf(dayReward));        
        response.put("rewardsWeek", String.valueOf(weekReward));
        response.put("rewardsMonth", String.valueOf(monthReward));
     
        response.put("lastBlock", JSONData.minimalBlock(Nxt.getBlockchain().getLastBlock()));
        
        return response;
    }  

}
