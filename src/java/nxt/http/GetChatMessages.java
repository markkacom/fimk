package nxt.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Appendix;
import nxt.MofoChat;
import nxt.Nxt;
import nxt.Transaction;
import nxt.UnconfirmedTransaction;
import nxt.Account.AccountInfo;
import nxt.db.DbIterator;
import nxt.util.Convert;
import nxt.util.JSON;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetChatMessages extends APIServlet.APIRequestHandler{

    static final GetChatMessages instance = new GetChatMessages();

    private GetChatMessages() {
        super(new APITag[] {APITag.MESSAGES}, "accountOne", "accountTwo", "firstIndex", "lastIndex");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long accountOneId;
        String accountOneValue = Convert.emptyToNull(req.getParameter("accountOne"));
        if (accountOneValue == null) {
            accountOneId = 0;
        }
        else {
            try {
                accountOneId = Convert.parseAccountId(accountOneValue);
            } catch (RuntimeException e) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"accountOne\"");
                throw new ParameterException(JSON.prepare(response));
            }
        }
        
        long accountTwoId;
        String accountTwoValue = Convert.emptyToNull(req.getParameter("accountTwo"));
        if (accountTwoValue == null) {
            accountTwoId = 0;
        }
        else {
            try {
                accountTwoId = Convert.parseAccountId(accountTwoValue);
            } catch (RuntimeException e) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"accountTwo\"");
                throw new ParameterException(JSON.prepare(response));
            }
        } 
        
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        List<Transaction> transactions = new ArrayList<Transaction>();
        if (firstIndex == 0) {
            try (DbIterator<? extends Transaction> iterator = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
                while (iterator.hasNext()) {
                    Transaction transaction = iterator.next();
                    if (transaction.getType().getType() == 1 && transaction.getType().getSubtype() == 0) {
                        if ((transaction.getSenderId() == accountOneId && transaction.getRecipientId() == accountTwoId) || 
                             transaction.getSenderId() == accountTwoId && transaction.getRecipientId() == accountOneId) 
                        {
                            transactions.add(transaction);
                        }
                    }
                }
            }
        }
        
        try (DbIterator<? extends Transaction> iterator = MofoChat.getChatTransactions(accountOneId, accountTwoId, firstIndex, lastIndex)) {
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
        response.put("accountOneRS", Convert.rsAccount(accountOneId));
        Account accountOne = Account.getAccount(accountOneId);
        if (accountOne != null) {
            AccountInfo info = accountOne.getAccountInfo();
            if (info != null) {
                response.put("accountOneName", info.getName());
            }
            if (accountOne.getPublicKey() != null) {
                response.put("accountOnePublicKey", Convert.toHexString(accountOne.getPublicKey()));
            }
        }
        
        response.put("accountTwoRS", Convert.rsAccount(accountTwoId));
        Account accountTwo = Account.getAccount(accountTwoId);
        if (accountTwo != null) {
            AccountInfo info = accountTwo.getAccountInfo();
            if (info != null) {
                response.put("accountTwoName", info.getName());
            }
            if (accountOne.getPublicKey() != null) {
                response.put("accountTwoPublicKey", Convert.toHexString(accountTwo.getPublicKey()));
            }
        }
        
        response.put("messages", list);
        return response;
    }
}
