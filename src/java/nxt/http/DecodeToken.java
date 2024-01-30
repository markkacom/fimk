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
import nxt.Token;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.*;

@Path("/fimk?requestType=decodeToken")
public final class DecodeToken extends APIServlet.APIRequestHandler {

    static final DecodeToken instance = new DecodeToken();

    private DecodeToken() {
        super(new APITag[]{APITag.TOKENS}, "website", "token");
    }

    @Override
    @GET
    @Operation(summary = "Decode token",
            tags = {APITag2.TOKEN})
    @Parameter(name = "website", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "token", in = ParameterIn.QUERY, required = true)
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) {

        String website = req.getParameter("website");
        String tokenString = req.getParameter("token");

        if (website == null) return MISSING_WEBSITE;

        if (tokenString == null) return MISSING_TOKEN;

        try {
            Token token = Token.parseToken(tokenString, website.trim());
            return JSONData.token(token);
        } catch (RuntimeException e) {
            return INCORRECT_WEBSITE;
        }
    }

}
