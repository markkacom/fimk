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
import nxt.Alias;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getAlias")
public final class GetAlias extends APIServlet.APIRequestHandler {

    static final GetAlias instance = new GetAlias();

    private GetAlias() {
        super(new APITag[] {APITag.ALIASES}, "alias", "aliasName");
    }

    @Override
    @Operation(summary = "Get alias",
            tags = {APITag2.ALIASES})
    @Parameter(name = "alias", in = ParameterIn.QUERY, description = "alias id")
    @Parameter(name = "aliasName", in = ParameterIn.QUERY, description = "alias name")
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        Alias alias = ParameterParser.getAlias(req);
        return JSONData.alias(alias);
    }

}
