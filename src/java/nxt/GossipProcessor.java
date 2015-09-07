package nxt;

import org.json.simple.JSONObject;

import nxt.util.Observable;

public interface GossipProcessor extends Observable<Gossip, GossipProcessor.Event> {

    public static enum Event {
        ADDED_GOSSIP
    }
 
    void processPeerGossip(JSONObject request) throws NxtException.ValidationException;
    
    void broadcast(Gossip gossip);
}
