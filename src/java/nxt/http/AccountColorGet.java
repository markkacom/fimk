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

public final class AccountColorGet extends CreateTransaction {

    static final AccountColorGet instance = new AccountColorGet();

    private AccountColorGet() {
        super(new APITag[] {APITag.MOFO}, "accountColorId", "includeAccountInfo", "includeDescription");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        if (!AccountColor.getAccountColorEnabled()) {
            return nxt.http.JSONResponses.FEATURE_NOT_AVAILABLE;
        }

        boolean includeAccountInfo = req.getParameter("includeAccountInfo") == "true";
        boolean includeDescription = req.getParameter("includeDescription") == "true";
        long accountColorId = ParameterParser.getUnsignedLong(req, "accountColorId", true);
        AccountColor accountColor = AccountColor.getAccountColor(accountColorId);
        if (accountColor == null) {
          return nxt.http.JSONResponses.unknown("accountColor"); 
        }
        
        return JSONData.accountColor(accountColor, includeAccountInfo, includeDescription);
    }
}