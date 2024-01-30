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
import nxt.peer.Hallmark;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.INCORRECT_HALLMARK;
import static nxt.http.JSONResponses.MISSING_HALLMARK;

@Path("/fimk?requestType=decodeHallmark")
public final class DecodeHallmark extends APIServlet.APIRequestHandler {

    static final DecodeHallmark instance = new DecodeHallmark();

    private DecodeHallmark() {
        super(new APITag[] {APITag.TOKENS}, "hallmark");
    }

    @Override
    @GET
    @Operation(summary = "Decode hallmark",
            tags = {APITag2.TOKEN})
    @Parameter(name = "hallmark", in = ParameterIn.QUERY, required = true)
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) {

        String hallmarkValue = req.getParameter("hallmark");
        if (hallmarkValue == null) {
            return MISSING_HALLMARK;
        }

        try {

            Hallmark hallmark = Hallmark.parseHallmark(hallmarkValue);

            return JSONData.hallmark(hallmark);

        } catch (RuntimeException e) {
            return INCORRECT_HALLMARK;
        }
    }

}
