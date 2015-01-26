package nxt.http;

import java.util.List;

import nxt.Account;
import nxt.Block;
import nxt.MofoQueries;
import nxt.MofoQueries.RewardsStruct;
import nxt.Nxt;
import nxt.NxtException;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class MofoGetAccounts extends APIServlet.APIRequestHandler {

    static final MofoGetAccounts instance = new MofoGetAccounts();
    static final int SECONDS_24H = 24 * 60 * 60;
    static final int SECONDS_WEEK = 7 * SECONDS_24H;
    static final int SECONDS_MONTH = 30 * SECONDS_24H;
    
    private MofoGetAccounts() {
        super(new APITag[] {APITag.MOFO}, "account" );
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
      
        List<Long> account_ids = ParameterParser.getAccountIds(req);
        
        JSONArray accounts_json = new JSONArray();
        
        for (Long id : account_ids) {

          JSONObject json = new JSONObject();
          json.put("account", Convert.toUnsignedLong(id));         
          
          Account account = Account.getAccount(id);
          if (account == null) {
              json.put("balanceNQT", "0");
              json.put("unconfirmedBalanceNQT", "0");
              json.put("effectiveBalanceNXT", "0");
              json.put("forgedBalanceNQT", "0");
              json.put("guaranteedBalanceNQT", "0");
              
              json.put("name", "");
              json.put("accountRS", Convert.rsAccount(id));
              json.put("numberOfBlocks", "0");
              
              json.put("lastBlockHeight", 0);
              json.put("lastBlockTimestamp", 0);
              
              json.put("forgedBalanceTodayNQT", "");        
              json.put("forgedBalanceWeekNQT", "");
              json.put("forgedBalanceMonthNQT", "");
              json.put("forgedBalanceTodayCount", 0);        
              json.put("forgedBalanceWeekCount", 0);
              json.put("forgedBalanceMonthCount", 0); 
              
          } else {
              json.put("balanceNQT", String.valueOf(account.getBalanceNQT()));
              json.put("unconfirmedBalanceNQT", String.valueOf(account.getUnconfirmedBalanceNQT()));
              json.put("effectiveBalanceNXT", account.getEffectiveBalanceNXT());
              json.put("forgedBalanceNQT", String.valueOf(account.getForgedBalanceNQT()));
              json.put("guaranteedBalanceNQT", String.valueOf(account.getGuaranteedBalanceNQT(1440)));
          
              json.put("name", account.getName());
              json.put("accountRS", Convert.rsAccount(account.getId()));
              json.put("numberOfBlocks", Nxt.getBlockchain().getBlockCount(account));
              
              Block block = MofoQueries.getLastBlock(account.getId());
              if (block != null) {
                json.put("lastBlockHeight", block.getHeight());
                json.put("lastBlockTimestamp", block.getTimestamp());
              }
              else {
                json.put("lastBlockHeight", 0);
                json.put("lastBlockTimestamp", 0);
              }
              
              int time = Nxt.getBlockchain().getLastBlock().getTimestamp();
              RewardsStruct dayReward = MofoQueries.getBlockRewardsSince(account.getId(), time - SECONDS_24H);
              RewardsStruct weekReward = MofoQueries.getBlockRewardsSince(account.getId(), time - SECONDS_WEEK);
              RewardsStruct monthReward = MofoQueries.getBlockRewardsSince(account.getId(), time - SECONDS_MONTH);
              
              json.put("forgedBalanceTodayNQT", String.valueOf(dayReward.getTotalRewardsNQT()));        
              json.put("forgedBalanceWeekNQT", String.valueOf(weekReward.getTotalRewardsNQT()));
              json.put("forgedBalanceMonthNQT", String.valueOf(monthReward.getTotalRewardsNQT()));
              json.put("forgedBalanceTodayCount", dayReward.getCount());        
              json.put("forgedBalanceWeekCount", weekReward.getCount());
              json.put("forgedBalanceMonthCount", monthReward.getCount());          
          
          }         
          accounts_json.add(json);
        }
        
        JSONObject response = new JSONObject();
        response.put("accounts", accounts_json);
        return response;
    }  

}
