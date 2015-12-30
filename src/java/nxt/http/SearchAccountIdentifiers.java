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

import nxt.Account;
import nxt.Account.AccountInfo;
import nxt.crypto.Crypto;
import nxt.db.DbIterator;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SearchAccountIdentifiers extends APIServlet.APIRequestHandler {

    static final SearchAccountIdentifiers instance = new SearchAccountIdentifiers();

    private SearchAccountIdentifiers() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.SEARCH}, "query", "accountColorId", "firstIndex", "lastIndex");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        String query = Convert.nullToEmpty(req.getParameter("query"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        long accountColorId = ParameterParser.getUnsignedLong(req, "accountColorId", false);

        JSONObject response = new JSONObject();
        JSONArray accountsJSONArray = new JSONArray();
        try (DbIterator<Account.AccountIdentifier> identifiers = Account.searchAccountIdentifiers(query, accountColorId, firstIndex, lastIndex)) {
            for (Account.AccountIdentifier identifier : identifiers) {
                JSONObject accountJSON = new JSONObject();
                String rsFormatted = Crypto.rsEncode(identifier.getAccountId());
                accountJSON.put("accountRS", "FIM-"+rsFormatted);
                if (!rsFormatted.equals(identifier.getEmail())) {
                  accountJSON.put("identifier", identifier.getEmail());
                }
                accountsJSONArray.add(accountJSON);
            }
        }
        response.put("accounts", accountsJSONArray);
        return response;
    }

}
