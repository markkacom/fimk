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
import nxt.Generator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;

@Path("/fimk?requestType=startForging")
public final class StartForging extends APIServlet.APIRequestHandler {

    static final StartForging instance = new StartForging();

    private StartForging() {
        super(new APITag[] {APITag.FORGING}, "secretPhrase");
    }

    @Override
    @POST
    @Operation(summary = "Start forging",
            tags = {APITag2.FORGING})
    @Parameter(name = "secretPhrase", in = ParameterIn.QUERY, required = true, description = "secret phrase")
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }

        Object result = Generator.startForging(secretPhrase);
        if (result instanceof String) {
            return JSONResponses.error((String) result);
        }
        Generator generator = (Generator) result;

        JSONObject response = new JSONObject();
        response.put("deadline", generator.getDeadline());
        response.put("hitTime", generator.getHitTime());
        return response;

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
