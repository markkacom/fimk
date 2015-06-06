package nxt.http;

import nxt.NxtException;
import nxt.http.websocket.MofoSocketServer;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class StopCollectingWebsocketEvents extends APIServlet.APIRequestHandler {

    static final StopCollectingWebsocketEvents instance = new StopCollectingWebsocketEvents();

    private StopCollectingWebsocketEvents() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        MofoSocketServer.stopCollectingEvents();
        JSONObject response = new JSONObject();
        return response;
    }

}
