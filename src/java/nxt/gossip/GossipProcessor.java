package nxt.gossip;

import java.util.Set;

import org.json.simple.JSONObject;

import nxt.NxtException;
import nxt.util.Observable;

public interface GossipProcessor extends Observable<Gossip, GossipProcessor.Event> {

    public static enum Event {
        ADDED_GOSSIP
    }

    void processPeerGossip(JSONObject request) throws NxtException.ValidationException;

    void broadcast(Gossip gossip);

    Set<Long> getExistingIds();
}
