package nxt.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Appendix;
import nxt.Constants;
import nxt.MofoChat;
import nxt.Nxt;
import nxt.Transaction;
import nxt.UnconfirmedTransaction;
import nxt.crypto.Crypto;
import nxt.db.DbIterator;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetChatMessages extends APIServlet.APIRequestHandler{

    static final GetChatMessages instance = new GetChatMessages();

    private GetChatMessages() {
        super(new APITag[] {APITag.MESSAGES}, "accountOne", "accountTwo", "firstIndex", "lastIndex");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        Account accountOne = ParameterParser.getAccount(req, "accountOne");
        Account accountTwo = ParameterParser.getAccount(req, "accountTwo");
        
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        List<Transaction> transactions = new ArrayList<Transaction>();
        if (firstIndex == 0) {
            try (DbIterator<? extends Transaction> iterator = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
                while (iterator.hasNext()) {
                    Transaction transaction = iterator.next();
                    if (transaction.getType().getType() == 1 && transaction.getType().getSubtype() == 0) {
                        if ((transaction.getSenderId() == accountOne.getId() && transaction.getRecipientId() == accountTwo.getId()) || 
                             transaction.getSenderId() == accountTwo.getId() && transaction.getRecipientId() == accountOne.getId()) 
                        {
                            transactions.add(transaction);
                        }
                    }
                }
            }
        }
        
        try (DbIterator<? extends Transaction> iterator = MofoChat.getChatTransactions(accountOne.getId(), accountTwo.getId(), firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                transactions.add(iterator.next());
            }
        }

        JSONArray list = new JSONArray();
        for (Transaction transaction : transactions) {
                
            JSONObject json = new JSONObject();
            if (transaction instanceof UnconfirmedTransaction) {
                json.put("confirmations", -1);
            }
            else {
                json.put("confirmations", Nxt.getBlockchain().getHeight() - transaction.getHeight());
            }
            json.put("recipientRS", Convert.rsAccount(transaction.getRecipientId()));
            json.put("senderRS", Convert.rsAccount(transaction.getSenderId()));
            json.put("timestamp", transaction.getTimestamp());
            json.put("transaction", transaction.getStringId());
            json.put("confirmations", Nxt.getBlockchain().getHeight() - transaction.getHeight());
            
            if (Constants.TRANSIENT_FULL_HASH.equals(transaction.getReferencedTransactionFullHash())) {
                json.put("transient", true);
            }

            JSONObject attachmentJSON = new JSONObject();
            for (Appendix appendage : transaction.getAppendages()) {
                attachmentJSON.putAll(appendage.getJSONObject());
            }
            if (! attachmentJSON.isEmpty()) {
                for (Map.Entry entry : (Iterable<Map.Entry>) attachmentJSON.entrySet()) {
                    if (entry.getValue() instanceof Long) {
                        entry.setValue(String.valueOf(entry.getValue()));
                    }
                }
                json.put("attachment", attachmentJSON);
            }                
            list.add(json);
        }
        
        JSONObject response = new JSONObject();
        response.put("accountOneRS", Convert.rsAccount(accountOne.getId()));
        response.put("accountOneName", accountOne.getName());
        if (accountOne.getPublicKey() != null) {
          response.put("accountOnePublicKey", Convert.toHexString(accountOne.getPublicKey()));
        }
        
        response.put("accountTwoRS", Convert.rsAccount(accountTwo.getId()));
        response.put("accountTwoName", accountTwo.getName());
        if (accountOne.getPublicKey() != null) {
          response.put("accountTwoPublicKey", Convert.toHexString(accountTwo.getPublicKey()));
        }
        
        response.put("messages", list);
        return response;
    }
}
