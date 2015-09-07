package nxt;

import nxt.NxtException.ValidationException;
import nxt.peer.Peers;
import nxt.util.Listener;
import nxt.util.Listeners;
import org.json.simple.JSONObject;

public class GossipProcessorImpl implements GossipProcessor {

    private static final GossipProcessorImpl instance = new GossipProcessorImpl();
    private final Listeners<Gossip, Event> gossipListeners = new Listeners<>();
  
    public static GossipProcessorImpl getInstance() {
        return instance;
    }
    
    @Override
    public boolean addListener(Listener<Gossip> listener, Event eventType) {
        return gossipListeners.addListener(listener, eventType);
    }
  
    @Override
    public boolean removeListener(Listener<Gossip> listener, Event eventType) {
        return gossipListeners.removeListener(listener, eventType);
    }
  
    @Override
    public void processPeerGossip(JSONObject request) throws ValidationException {
        if (!Peers.gossipEnabled) {
            throw new RuntimeException("Gossip not enabled");
        }
        Gossip gossip = GossipImpl.parseGossip(request);
        if (gossip != null) {
            gossip.remember();
            gossipListeners.notify(gossip, GossipProcessor.Event.ADDED_GOSSIP);
            Peers.sendToSomePeers(gossip);
        }
    }

    @Override
    public void broadcast(Gossip gossip) {
        if (!Peers.gossipEnabled) {
            throw new RuntimeException("Gossip not enabled");
        }
        gossip.remember();
        gossipListeners.notify(gossip, GossipProcessor.Event.ADDED_GOSSIP);
        Peers.sendToSomePeers(gossip);
    }
}
