/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
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

import nxt.Account;
import nxt.db.DbIterator;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SearchAccountIdentifiers extends APIServlet.APIRequestHandler {

    static final SearchAccountIdentifiers instance = new SearchAccountIdentifiers();

    private SearchAccountIdentifiers() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.SEARCH}, "query", "firstIndex", "lastIndex");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        try (DbIterator<Account.AccountIdentifier> identifiers = Account.searchAccountIdentifiers(query, firstIndex, lastIndex)) {
            for (Account.AccountIdentifier identifier : identifiers) {
                JSONObject accountJSON = new JSONObject();
                accountJSON.put("accountRS", Convert.rsAccount(identifier.getAccountId()));
                accountJSON.put("identifier", identifier.getEmail());
                accountsJSONArray.add(accountJSON);
            }
        }
        response.put("accounts", accountsJSONArray);
        return response;
    }

}
