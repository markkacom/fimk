package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.NxtException;
import nxt.virtualexchange.VirtualTrade;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.List;

@Path("/fimk?requestType=getVirtualTrades")
public final class GetVirtualTrades extends APIServlet.APIRequestHandler {

    static final GetVirtualTrades instance = new GetVirtualTrades();

    private GetVirtualTrades() {
        super(new APITag[] {APITag.AE}, "asset", "firstIndex", "lastIndex");
    }

    @Override
    @Operation(summary = "Get virtual trades",
            tags = {APITag2.AE})
    @Parameter(name = "asset", in = ParameterIn.QUERY, required = true, description = "asset id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long assetId = ParameterParser.getAsset(req).getId();
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
  
        JSONArray json = new JSONArray();
        List<VirtualTrade> trades = VirtualTrade.getTrades(assetId, firstIndex, lastIndex);
        for (VirtualTrade trade : trades) {
            json.add(trade.toJSONObject());
        }
  
        JSONObject response = new JSONObject();
        response.put("trades", json);
        return response;

    }

}
