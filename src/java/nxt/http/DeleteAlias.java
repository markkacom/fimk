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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nxt.Account;
import nxt.Alias;
import nxt.Attachment;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.INCORRECT_ALIAS_OWNER;


@Path("/fimk?requestType=deleteAlias")
public final class DeleteAlias extends CreateTransaction {

    static final DeleteAlias instance = new DeleteAlias();

    private DeleteAlias() {
        super(new APITag[]{APITag.ALIASES, APITag.CREATE_TRANSACTION}, "alias", "aliasName");
    }

    @Override
    @Operation(summary = "Delete alias",
            tags = {APITag2.ALIASES, APITag2.CREATE_TRANSACTION})
    @Parameter(name = "alias", in = ParameterIn.QUERY, description = "alias id")
    @Parameter(name = "aliasName", in = ParameterIn.QUERY, description = "alias name")
    public JSONStreamAware processRequest(@Parameter(hidden = true) final HttpServletRequest req) throws NxtException {
        final Alias alias = ParameterParser.getAlias(req);
        final Account owner = ParameterParser.getSenderAccount(req);

        if (alias.getAccountId() != owner.getId()) {
            return INCORRECT_ALIAS_OWNER;
        }

        final Attachment attachment = new Attachment.MessagingAliasDelete(alias.getAliasName());
        return createTransaction(req, owner, attachment);
    }
}
