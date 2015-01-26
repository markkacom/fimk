package nxt.http;

import java.util.List;

import nxt.MofoQueries;
import nxt.MofoQueries.ForgingStatStruct;
import nxt.Nxt;
import nxt.NxtException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class MofoGetForgingStats extends APIServlet.APIRequestHandler {

    static final MofoGetForgingStats instance = new MofoGetForgingStats();

    private MofoGetForgingStats() {
        super(new APITag[] {APITag.MOFO}, "timestamp" );
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        int timestamp = ParameterParser.getTimestamp(req);
        if (timestamp == 0) {
            timestamp = Nxt.getBlockchain().getLastBlock().getTimestamp();
        }

        List<ForgingStatStruct> list = MofoQueries.getForgingStats24H(timestamp);
        
        JSONArray forgers = new JSONArray();
        for (ForgingStatStruct stat : list) {
            JSONObject json = new JSONObject();
            JSONData.putAccount(json, "account", stat.getAccountId());
            json.put("feeNQT", String.valueOf(stat.getTotalFeeNQT()));
            json.put("count", stat.getBlockCount());
            forgers.add(json);
        }
        
        JSONObject response = new JSONObject();
        response.put("forgers", forgers);
        return response;
    }  

}
