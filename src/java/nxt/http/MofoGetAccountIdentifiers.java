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

import io.swagger.v3.oas.annotations.Parameter;
import nxt.Account;
import nxt.Account.AccountIdentifier;
import nxt.NxtException;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

/* @api-name getAccountIdentifiers */
//@Path("/fimk?requestType=accountColorList")
public final class MofoGetAccountIdentifiers extends APIServlet.APIRequestHandler {

    static final MofoGetAccountIdentifiers instance = new MofoGetAccountIdentifiers();

    private MofoGetAccountIdentifiers() {
        super(new APITag[] {APITag.ACCOUNTS}, "account", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

        long accountId = ParameterParser.getAccountId(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);


        JSONArray result = new JSONArray();
        try (DbIterator<AccountIdentifier> identifiers = Account.getAccountIdentifiers(accountId, firstIndex, lastIndex)) {
            while (identifiers.hasNext()) {
                result.add(identifiers.next().getEmail());
            }
        }
        JSONObject response = new JSONObject();
        response.put("identifiers", result);
        return response;
    }

}
