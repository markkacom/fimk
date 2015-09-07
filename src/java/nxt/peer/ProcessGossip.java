package nxt.peer;

import nxt.Nxt;
import nxt.NxtException;
import nxt.util.JSON;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class ProcessGossip extends PeerServlet.PeerRequestHandler {

    static final ProcessGossip instance = new ProcessGossip();

    private ProcessGossip() {}

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        try {
            Nxt.getGossipProcessor().processPeerGossip(request);
            return JSON.emptyJSON;
        } catch (RuntimeException | NxtException.ValidationException e) {
            //Logger.logDebugMessage("Failed to parse peer transactions: " + request.toJSONString());
            peer.blacklist(e);
            JSONObject response = new JSONObject();
            response.put("error", e.toString());
            return response;
        }
    }
}
