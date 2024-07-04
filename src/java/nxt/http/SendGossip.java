package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.Account;
import nxt.NxtException.NotValidException;
import nxt.gossip.GossipImpl;
import nxt.gossip.GossipProcessorImpl;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;


@Path("/fimk?requestType=sendGossip")
public class SendGossip extends APIServlet.APIRequestHandler {

    static final SendGossip instance = new SendGossip();

    private SendGossip() {
        super(new APITag[] {APITag.MOFO}, "id", "message", "senderPublicKey", "topic", "timestamp", "signature");
    }

    @SuppressWarnings("unchecked")
    @Override
    @POST
    @Operation(summary = "Send gossip",
            tags = {APITag2.MOFO})
    @Parameter(name = "id", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"))
    @Parameter(name = "message", in = ParameterIn.QUERY)
    @Parameter(name = "senderPublicKey", in = ParameterIn.QUERY,
            description = "sender public key in HEX")
    @Parameter(name = "topic", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"), description = "topic id")
    @Parameter(name = "timestamp", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"), description = "timestamp")
    @Parameter(name = "signature", in = ParameterIn.QUERY, description = "signature in HEX")
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
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
