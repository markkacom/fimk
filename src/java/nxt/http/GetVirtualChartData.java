package nxt.http;

import nxt.Asset;
import nxt.MofoQueries;
import nxt.NxtException;
import nxt.util.JSON;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetVirtualChartData extends APIServlet.APIRequestHandler {

    static final GetVirtualChartData instance = new GetVirtualChartData();

    private GetVirtualChartData() {
        super(new APITag[] {APITag.AE}, "asset", "window");
    }

    static final byte TEN_MINUTES = 0;
    static final byte HOUR = 1;
    static final byte DAY  = 2;
    static final byte WEEK = 3;

    private int windowToSeconds(byte window) throws ParameterException {
        switch (window) {
            case TEN_MINUTES: return 600;
            case HOUR: return 3600;
            case DAY: return 86400;
            case WEEK: return 604800;
        }
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect window (window must be in [0..3] range");
        throw new ParameterException(JSON.prepare(response));
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Asset asset = ParameterParser.getAsset(req);
        byte window;
        try {
            window = Byte.parseByte(req.getParameter("window"));
        } catch (NumberFormatException e) {
            window = -1;
        }

        JSONArray chart_data = MofoQueries.getAssetChartData(asset.getId(), windowToSeconds(window));
        JSONObject response = new JSONObject();
        response.put("name", asset.getName());
        response.put("decimals", asset.getDecimals());
        response.put("data", chart_data);
        return response;
    }

}
