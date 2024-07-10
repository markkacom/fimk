package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.NxtException;
import nxt.util.Convert;
import nxt.virtualexchange.VirtualOrder.VirtualBid;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.List;

@Path("/fimk?requestType=getVirtualBidOrders")
public final class GetVirtualBidOrders extends APIServlet.APIRequestHandler {

    static final GetVirtualBidOrders instance = new GetVirtualBidOrders();

    private GetVirtualBidOrders() {
        super(new APITag[] {APITag.AE}, "asset", "firstIndex", "lastIndex", "account");
    }

    @SuppressWarnings("unchecked")
    @Override
    @Operation(summary = "Get virtual bid orders",
            tags = {APITag2.AE})
    @Parameter(name = "asset", in = ParameterIn.QUERY, required = true, description = "asset id")
    @Parameter(name = "account", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"),
            description = "account id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long assetId = ParameterParser.getAsset(req).getId();
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        
        String accountValue = Convert.emptyToNull(req.getParameter("account"));
        long accountId = 0;
        if (accountValue != null) {
            accountId = ParameterParser.getAccountId(req);
        }
  
        JSONArray orders = new JSONArray();
        List<VirtualBid> bidOrders = VirtualBid.getBids(assetId, firstIndex, lastIndex, accountId);
        for (VirtualBid bid : bidOrders) {
            orders.add(bid.toJSONObject());
        }
  
        JSONObject response = new JSONObject();
        response.put("bidOrders", orders);
        return response;

    }

}
