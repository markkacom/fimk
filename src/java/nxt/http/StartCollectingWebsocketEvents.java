package nxt.http;

import nxt.NxtException;
import nxt.http.websocket.MofoSocketServer;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class StartCollectingWebsocketEvents extends APIServlet.APIRequestHandler {

    static final StartCollectingWebsocketEvents instance = new StartCollectingWebsocketEvents();

    private StartCollectingWebsocketEvents() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        MofoSocketServer.startCollectingEvents(); 
        JSONObject response = new JSONObject();
        return response;
    }

}
