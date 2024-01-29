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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nxt.*;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.incorrect;
import static nxt.http.JSONResponses.missing;

@Path("/fimk?requestType=accountColorCreate")
public final class AccountColorCreate extends CreateTransaction {

    static final AccountColorCreate instance = new AccountColorCreate();

    private AccountColorCreate() {
        super(new APITag[] {APITag.MOFO, APITag.CREATE_TRANSACTION}, "name", "description");
    }

    @Override
    @POST
    @Operation(summary = "Create Account Color",
            tags = {APITag2.ACCOUNT, APITag2.CREATE_TRANSACTION},
            description = "User can create their own account color and create one or more initial colored accounts")
    @Parameter(name = "name", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "description", in = ParameterIn.QUERY)
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

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