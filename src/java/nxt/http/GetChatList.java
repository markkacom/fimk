package nxt.http;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.MofoChat;
import nxt.MofoChat.Chat;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetChatList extends APIServlet.APIRequestHandler{

    static final GetChatList instance = new GetChatList();

    private GetChatList() {
        super(new APITag[] {APITag.MESSAGES}, "account", "firstIndex", "lastIndex");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        Account account = ParameterParser.getAccount(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        
        List<Chat> chatList = MofoChat.getChatList(account.getId(), firstIndex, lastIndex);
        JSONArray chats = new JSONArray();

        for (Chat c : chatList) {
            JSONObject chat = new JSONObject();
            chat.put("accountRS", Convert.rsAccount(c.getAccountId()));
            Account other = Account.getAccount(c.getAccountId());
            if (other != null) {
                chat.put("accountName", other.getName());
            }
            chat.put("timestamp", c.getTimestamp());
            chats.add(chat);
        }

        JSONObject response = new JSONObject();
        response.put("accountRS",  Convert.rsAccount(account.getId()));
        response.put("accountName", account.getName());
        response.put("chats", chats);
        return response;
    }
}
