package nxt.gossip;

import org.json.simple.JSONObject;

public interface Gossip {

    JSONObject getJSONObject();

    long getSenderId();

    long getRecipientId();

    byte[] getMessage();
    
    void remember();

    long getTopic();

    int getTimestamp();

    long getId();

    byte[] getSignature();

    byte[] getSenderPublicKey();
}
