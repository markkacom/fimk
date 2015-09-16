package nxt.http;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.GossipImpl;
import nxt.GossipProcessorImpl;
import nxt.NxtException.NotValidException;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;


public class SendGossip extends APIServlet.APIRequestHandler {

    static final SendGossip instance = new SendGossip();

    private SendGossip() {
        super(new APITag[] {APITag.MOFO}, "id", "message", "senderPublicKey", "topic", "timestamp", "signature");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        JSONObject response = new JSONObject();
        try {
  
            long id = Long.parseUnsignedLong(Convert.emptyToNull(req.getParameter("id")));
            long recipientId = ParameterParser.getAccountId(req, "recipient", false);
            String messageValue = Convert.emptyToNull(req.getParameter("message"));
            String senderPublicKeyValue = Convert.emptyToNull(req.getParameter("senderPublicKey"));
            String topicValue = Convert.emptyToNull(req.getParameter("topic"));
            int timestamp = ParameterParser.getTimestamp(req);
            String signatureValue = Convert.emptyToNull(req.getParameter("signature"));
            
            byte[] message = Convert.parseHexString(messageValue);
            byte[] senderPublicKey = Convert.parseHexString(senderPublicKeyValue);
            byte[] signature = Convert.parseHexString(signatureValue);
      
            long topic = 0;
            if (topicValue != null) {
                topic = Long.parseUnsignedLong(topicValue);
            }
      
            long senderId = Account.getId(senderPublicKey);
            
            try {
                GossipImpl gossip = new GossipImpl(id, senderId, recipientId, message, topic, timestamp, senderPublicKey, signature);
                gossip.validate(true);
                GossipProcessorImpl.getInstance().broadcast(gossip);
                response.put("gossip", req.getParameter("id"));                
            } 
            catch (NotValidException e) {
                e.printStackTrace();
                response.put("error", e.toString());
            }
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            response.put("error", e.toString());
        }
        return response;
    }

    @Override
    final boolean requirePost() {
        return true;
    }
}
