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
import nxt.AccountColor;
import nxt.Attachment;
import nxt.Constants;
import nxt.MofoAttachment;
import nxt.NxtException;
import nxt.util.Convert;

import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.missing;
import static nxt.http.JSONResponses.incorrect;

public final class AccountColorCreate extends CreateTransaction {

    static final AccountColorCreate instance = new AccountColorCreate();

    private AccountColorCreate() {
        super(new APITag[] {APITag.MOFO, APITag.CREATE_TRANSACTION}, "name", "description");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        if (!AccountColor.getAccountColorEnabled()) {
            return nxt.http.JSONResponses.FEATURE_NOT_AVAILABLE;
        }

        String name = Convert.emptyToNull(req.getParameter("name"));
        String description = Convert.nullToEmpty(req.getParameter("description"));

        if (name == null) {
            return missing("name");
        }

        name = name.trim();
        if (name.length() == 0 || name.length() > Constants.MAX_ACCOUNT_COLOR_NAME_LENGTH) {
            return incorrect("name", "(length must be in [1.." + Constants.MAX_ACCOUNT_COLOR_NAME_LENGTH + "] range)");
        }

        String normalizedName = name.toLowerCase();
        for (int i = 0; i < normalizedName.length(); i++) {
            if (Constants.ALPHABET.indexOf(normalizedName.charAt(i)) < 0) {
                return incorrect("name", "(must contain only digits and latin letters)");
            }
        }

        description = description.trim();
        if (description.length() > Constants.MAX_ACCOUNT_COLOR_DESCRIPTION_LENGTH) {
            return incorrect("description", "(length must be in [1.." + Constants.MAX_ACCOUNT_COLOR_DESCRIPTION_LENGTH + "] range)");
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new MofoAttachment.AccountColorCreateAttachment(name, description);
        return createTransaction(req, account, attachment);
    }
}