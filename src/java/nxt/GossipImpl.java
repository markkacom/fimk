package nxt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import nxt.NxtException.NotValidException;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;

public class GossipImpl implements Gossip {

    private long id;
    private long sender;
    private byte[] senderPublicKey = null;
    private long recipient;
    private byte[] message;
    private long topic;
    private int timestamp;
    private byte[] signature = null;

    @SuppressWarnings("serial")
    private static final Set<Long> existingIds = Collections.newSetFromMap(new LinkedHashMap<Long, Boolean>(){
        
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
            return size() > Constants.MAX_GOSSIP_CACHE_LENGTH;
        }
    });    

    
    public GossipImpl(long id, long sender, long recipient, byte[] message, long topic, 
          int timestamp, byte[] senderPublicKey, byte[] signature) throws NotValidException {
        this.id = id;
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.topic = topic;
        this.timestamp = timestamp;
        this.senderPublicKey = senderPublicKey;
        this.signature = signature;
    }
    
    @Override
    public void remember() {
        existingIds.add(id);
    }

    static GossipImpl parseGossip(JSONObject gossipData) throws NxtException.NotValidException {
        long id = Long.parseUnsignedLong((String) gossipData.get("id"));
        byte[] senderPublicKey = Convert.parseHexString((String) gossipData.get("senderPublicKey"));
        long sender = Account.getId(senderPublicKey);      
        long recipient = Long.parseUnsignedLong((String) gossipData.get("recipient"));
     
        byte[] message = Convert.parseHexString((String) gossipData.get("message"));        
        long topic;
        String topicValue = Convert.emptyToNull((String) gossipData.get("topic"));
        if (topicValue == null) {
            topic = 0;
        }
        else {
            topic = Long.parseUnsignedLong(topicValue);
        }
        int timestamp = ((Long) gossipData.get("timestamp")).intValue();
        byte[] signature = Convert.parseHexString((String) gossipData.get("signature"));
        
        GossipImpl gossip = new GossipImpl(id, sender, recipient, message, topic, timestamp, senderPublicKey, signature);
        try {
            /* will throw if gossip exists or if gossip is to old */
            gossip.validate(true);
        }
        catch (NxtException.NotValidException e) {
            return null;
        }
        return gossip;
    }

    /**
     * @param new_gossip indicating this gossip was either received from another peer
     *                   or that it was just created by the client user.
     */
    public void validate(boolean new_gossip) throws NxtException.NotValidException {

        if (senderPublicKey == null) {
            throw new NxtException.NotValidException("Gossip senderPublicKey cannot be empty");
        }
        
        if (signature == null) {
            throw new NxtException.NotValidException("Gossip signature cannot be empty");
        }
        
        if (message == null) {
            throw new NxtException.NotValidException("Gossip message cannot be empty");
        }

        if (id != getId(signature)) {
            throw new NxtException.NotValidException("Invalid gossip id");
        }        

        if (message.length > Constants.MAX_GOSSIP_MESSAGE_LENGTH) {
            throw new NxtException.NotValidException("Invalid gossip message length: " + message.length);
        }

        if (new_gossip) {
            if (existingIds.contains(id)) {
                throw new NxtException.NotValidException("Gossip id exists");
            }
    
            int curTime = Nxt.getEpochTime();
            if (timestamp > curTime + Constants.MAX_GOSSIP_TIMEDRIFFT) {
                throw new NxtException.NotValidException("Invalid gossip timestamp");
            }
        }
        
        byte[] signatureSeed = createSignatureSeed(timestamp, Long.toUnsignedString(recipient), 
            Convert.toHexString(message), Long.toUnsignedString(topic));

        if (!Crypto.verify(signature, signatureSeed, senderPublicKey, false)) {
            throw new NxtException.NotValidException("Invalid gossip signature");
        }      
    }

    public static byte[] createSignatureSeed(int timestamp, String recipient, String message, String topic) {
        StringBuilder signatureSeed = new StringBuilder();
        signatureSeed.append(timestamp);
        signatureSeed.append(recipient);
        signatureSeed.append(message);
        signatureSeed.append(topic);
        return Convert.toBytes(signatureSeed.toString());
    }

    static long getId(byte[] signature) {
        byte[] signatureHash = Crypto.sha256().digest(signature);
        return Convert.fullHashToId(signatureHash);
    }    

    @Override
    public byte[] getSignature() {
        return signature;
    }
    
    @Override
    public byte[] getSenderPublicKey() {
        return senderPublicKey;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public byte[] getMessage() {
        return message;
    }
    
    @Override
    public long getRecipientId() {
        return recipient;
    }
    
    @Override
    public long getSenderId() {
        return sender;
    }

    @Override
    public long getTopic() {
        return topic;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("id", Long.toUnsignedString(id));
        json.put("senderPublicKey", Convert.toHexString(senderPublicKey));
        json.put("recipient", Long.toUnsignedString(recipient));
        json.put("message", Convert.toHexString(message));
        json.put("topic", Long.toUnsignedString(topic));
        json.put("timestamp", timestamp);
        json.put("signature", Convert.toHexString(signature));
        return json;
    }
}
