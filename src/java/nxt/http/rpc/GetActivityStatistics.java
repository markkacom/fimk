package nxt.http.rpc;

import nxt.MofoQueries;
import nxt.Nxt;
import nxt.http.websocket.JSONData;
import nxt.http.ParameterException;
import nxt.http.websocket.RPCCall;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetActivityStatistics extends RPCCall {
  
    public static RPCCall instance = new GetActivityStatistics("getActivityStatistics");
    static final int SECONDS_24H = 24 * 60 * 60;
    static final int SECONDS_WEEK = 7 * SECONDS_24H;
    static final int SECONDS_MONTH = 30 * SECONDS_24H;  
  
    public GetActivityStatistics(String identifier) {
        super(identifier);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {

        JSONObject response = new JSONObject();        
        int time = Nxt.getBlockchain().getLastBlock().getTimestamp();
      
        response.put("averageBlockTime24H", MofoQueries.getAverageBlockTime24H());
        response.put("transactionCountToday", MofoQueries.getTransactionCountSince(time - SECONDS_24H));
        response.put("transactionCountWeek", MofoQueries.getTransactionCountSince(time - SECONDS_WEEK));
        response.put("transactionCountMonth", MofoQueries.getTransactionCountSince(time - SECONDS_MONTH));
        
        // TODO - Instead of using single queries try to do this with one single query
        long dayReward = MofoQueries.getBlockRewardsSince(time - SECONDS_24H).getTotalRewardsNQT();
        long weekReward = MofoQueries.getBlockRewardsSince(time - SECONDS_WEEK).getTotalRewardsNQT();
        long monthReward = MofoQueries.getBlockRewardsSince(time - SECONDS_MONTH).getTotalRewardsNQT();

        response.put("rewardsToday", String.valueOf(dayReward));        
        response.put("rewardsWeek", String.valueOf(weekReward));
        response.put("rewardsMonth", String.valueOf(monthReward));
     
        response.put("lastBlock", JSONData.minimalBlock(Nxt.getBlockchain().getLastBlock()));
        
        return response;
    }  

}
