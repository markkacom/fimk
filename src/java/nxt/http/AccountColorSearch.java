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
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class AccountColorSearch extends CreateTransaction {

    static final AccountColorSearch instance = new AccountColorSearch();

    private AccountColorSearch() {
        super(new APITag[] {APITag.MOFO}, "account", "name", "firstIndex", "lastIndex", "includeAccountInfo", "includeDescription");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        if (!AccountColor.getAccountColorEnabled()) {
            return nxt.http.JSONResponses.FEATURE_NOT_AVAILABLE;
        }

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        long accountId = ParameterParser.getAccountId(req, "account", false);
        boolean includeAccountInfo = req.getParameter("includeAccountInfo") == "true";
        boolean includeDescription = req.getParameter("includeDescription") == "true";
        String name = Convert.nullToEmpty(req.getParameter("name"));

        JSONObject response = new JSONObject();
        JSONArray colors = new JSONArray();
        response.put("accountColors", colors);

        try (DbIterator<AccountColor> iterator = AccountColor.searchAccountColorsByName(name, accountId, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                colors.add(JSONData.accountColor(iterator.next(), includeAccountInfo, includeDescription));
            }
        }

        return response;
    }
}