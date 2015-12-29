/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
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

public final class AccountColorList extends CreateTransaction {

    static final AccountColorList instance = new AccountColorList();

    private AccountColorList() {
        super(new APITag[] {APITag.MOFO}, "query", "account", "firstIndex", "lastIndex", "includeAccountInfo", "includeDescription");
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
        String query = Convert.nullToEmpty(req.getParameter("query")).trim();

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