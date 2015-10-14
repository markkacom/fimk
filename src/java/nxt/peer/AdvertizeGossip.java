package nxt.peer;

import nxt.util.JSON;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class AdvertizeGossip extends PeerServlet.PeerRequestHandler {

    static final AdvertizeGossip instance = new AdvertizeGossip();

    private AdvertizeGossip() {}

    @Override
    JSONStreamAware processRequest(final JSONObject request, final Peer peer) {
        return JSON.emptyJSON;
    }

}
