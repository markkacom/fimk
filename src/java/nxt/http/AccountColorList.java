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

import nxt.AccountColor;
import nxt.NxtException;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class AccountColorList extends APIServlet.APIRequestHandler {

    static final AccountColorList instance = new AccountColorList();

    private AccountColorList() {
        super(new APITag[] {APITag.MOFO}, "query", "account", "firstIndex", "lastIndex", "includeAccountInfo", "includeDescription");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        long accountId = ParameterParser.getAccountId(req, "account", false);
        boolean includeAccountInfo = "true".equals(req.getParameter("includeAccountInfo"));
        boolean includeDescription = "true".equals(req.getParameter("includeDescription"));

        JSONObject response = new JSONObject();
        JSONArray colors = new JSONArray();
        response.put("accountColors", colors);

        try (DbIterator<AccountColor> iterator = accountId == 0 ?
                AccountColor.getAllAccountColors(firstIndex, lastIndex) :
                AccountColor.getAccountColorsIssuedBy(accountId, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                colors.add(JSONData.accountColor(iterator.next(), includeAccountInfo, includeDescription));
            }
        }

        return response;
    }
}