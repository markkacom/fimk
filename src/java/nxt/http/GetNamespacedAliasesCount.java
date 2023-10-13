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

import nxt.NamespacedAlias;
import nxt.util.JSON;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetNamespacedAliasesCount extends APIServlet.APIRequestHandler {

    static final GetNamespacedAliasesCount instance = new GetNamespacedAliasesCount();

    private GetNamespacedAliasesCount() {
        super(new APITag[] {APITag.ALIASES}, "account", "filter");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        final long accountId = ParameterParser.getAccountId(req);
        final String filter = ParameterParser.getFilter(req);

        if (filter == null && accountId == 0) {
            JSONObject response = new JSONObject();
            response.put("errorCode", 4);
            response.put("errorDescription", "Must provide 'account' if omitting 'filter'");
            throw new ParameterException(JSON.prepare(response));
        }

        JSONObject response = new JSONObject();
        response.put("count", (filter == null) ?
                NamespacedAlias.getAccountAliasCount(accountId) :
                NamespacedAlias.searchAliasesCount(filter, accountId));
        return response;
    }

}