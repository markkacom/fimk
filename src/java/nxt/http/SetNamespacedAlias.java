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
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.*;

@Path("/fimk?requestType=setNamespacedAlias")
public final class SetNamespacedAlias extends CreateTransaction {

    static final SetNamespacedAlias instance = new SetNamespacedAlias();

    private SetNamespacedAlias() {
        super(new APITag[] {APITag.ALIASES, APITag.CREATE_TRANSACTION}, "aliasName", "aliasURI");
    }

    @Override
    @Operation(summary = "Set namespaced alias",
            tags = {APITag2.ALIASES, APITag2.CREATE_TRANSACTION})
    @Parameter(name = "aliasName", in = ParameterIn.QUERY, required = true, description = "alias name")
    @Parameter(name = "aliasURI", in = ParameterIn.QUERY,
            description = "alias URI")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String aliasName = Convert.emptyToNull(req.getParameter("aliasName"));
        String aliasURI = Convert.nullToEmpty(req.getParameter("aliasURI"));

        if (aliasName == null) {
            return MISSING_ALIAS_NAME;
        }

        aliasName = aliasName.trim();
        if (aliasName.length() == 0 || aliasName.length() > Constants.MAX_ALIAS_LENGTH) {
            return INCORRECT_ALIAS_LENGTH;
        }

        String normalizedAlias = aliasName.toLowerCase();
        for (int i = 0; i < normalizedAlias.length(); i++) {
            if (Constants.NAMESPACED_ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                return INCORRECT_ALIAS_NAME;
            }
        }

        aliasURI = aliasURI.trim();
        if (aliasURI.length() > Constants.MAX_ALIAS_URI_LENGTH) {
            return INCORRECT_URI_LENGTH;
        }

        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new MofoAttachment.NamespacedAliasAssignmentAttachment(aliasName, aliasURI);
        return createTransaction(req, account, attachment);

    }

}
