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
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class AccountColorGet extends APIServlet.APIRequestHandler {

    static final AccountColorGet instance = new AccountColorGet();

    private AccountColorGet() {
        super(new APITag[] {APITag.MOFO}, "accountColorId", "includeAccountInfo", "includeDescription");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        boolean includeAccountInfo = "true".equals(req.getParameter("includeAccountInfo"));
        boolean includeDescription = "true".equals(req.getParameter("includeDescription"));
        long accountColorId = ParameterParser.getUnsignedLong(req, "accountColorId", true);
        AccountColor accountColor = AccountColor.getAccountColor(accountColorId);
        if (accountColor == null) {
            return nxt.http.JSONResponses.unknown("accountColor");
        }

        return JSONData.accountColor(accountColor, includeAccountInfo, includeDescription);
    }
}