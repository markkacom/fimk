/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.Account;
import nxt.Account.AccountInfo;
import nxt.MofoChat;
import nxt.MofoChat.Chat;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.List;

@Path("/fimk?requestType=getChatList")
public class GetChatList extends APIServlet.APIRequestHandler{

    static final GetChatList instance = new GetChatList();

    private GetChatList() {
        super(new APITag[] {APITag.MESSAGES}, "account", "firstIndex", "lastIndex");
    }

    @SuppressWarnings("unchecked")
    @Override
    @Operation(summary = "Get chat list",
            tags = {APITag2.MESSAGES})
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true, description = "account id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long accountId = ParameterParser.getAccountId(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        List<Chat> chatList = MofoChat.getChatList(accountId, firstIndex, lastIndex);
        JSONArray chats = new JSONArray();

        for (Chat c : chatList) {
            JSONObject chat = new JSONObject();
            chat.put("accountRS", Convert.rsAccount(c.getAccountId()));
            Account other = Account.getAccount(c.getAccountId());
            if (other != null) {
                AccountInfo info = other.getAccountInfo();
                if (info != null) {
                    chat.put("accountName", info.getName());
                }
            }
            chat.put("timestamp", c.getTimestamp());
            chats.add(chat);
        }

        JSONObject response = new JSONObject();
        response.put("accountRS",  Convert.rsAccount(accountId));
        
        Account account = Account.getAccount(accountId);
        if (account != null) {
            AccountInfo info = account.getAccountInfo();
            if (info != null) {          
                response.put("accountName", info.getName());
            }
        }
        response.put("chats", chats);
        return response;
    }
}
