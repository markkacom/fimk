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
import nxt.NamespacedAlias;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getNamespacedAlias")
public final class GetNamespacedAlias extends APIServlet.APIRequestHandler {

    static final GetNamespacedAlias instance = new GetNamespacedAlias();

    private GetNamespacedAlias() {
        super(new APITag[] {APITag.ALIASES}, "account", "alias", "aliasName");
    }

    @Override
    @Operation(summary = "Get namespaced alias",
            tags = {APITag2.ALIASES})
    @Parameter(name = "account", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"),
            description = "account id")
    @Parameter(name = "alias", in = ParameterIn.QUERY, required = true, description = "alias")
    @Parameter(name = "aliasName", in = ParameterIn.QUERY, required = true, description = "alias name")
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        NamespacedAlias alias = ParameterParser.getNamespacedAlias(req);
        return JSONData.namespacedAlias(alias);
    }

}
