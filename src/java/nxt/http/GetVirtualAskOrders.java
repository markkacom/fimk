package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.NxtException;
import nxt.util.Convert;
import nxt.virtualexchange.VirtualOrder.VirtualAsk;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.List;

@Path("/fimk?requestType=getVirtualAskOrders")
public final class GetVirtualAskOrders extends APIServlet.APIRequestHandler {

    static final GetVirtualAskOrders instance = new GetVirtualAskOrders();

    private GetVirtualAskOrders() {
        super(new APITag[] {APITag.AE}, "asset", "firstIndex", "lastIndex", "account");
    }

    @SuppressWarnings("unchecked")
    @Override
    @Operation(summary = "Get virtual ask orders",
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
        List<VirtualAsk> askOrders = VirtualAsk.getAsks(assetId, firstIndex, lastIndex, accountId);
        for (VirtualAsk ask : askOrders) {
            orders.add(ask.toJSONObject());
        }

        JSONObject response = new JSONObject();
        response.put("askOrders", orders);
        return response;

    }

}
