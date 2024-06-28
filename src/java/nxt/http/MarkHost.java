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
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.Constants;
import nxt.peer.Hallmark;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.*;


@Path("/fimk?requestType=markHost")
public final class MarkHost extends APIServlet.APIRequestHandler {

    static final MarkHost instance = new MarkHost();

    private MarkHost() {
        super(new APITag[] {APITag.TOKENS}, "secretPhrase", "host", "weight", "date");
    }

    @Override
    @POST
    @Operation(summary = "Mark host",
            tags = {APITag2.TOKEN})
    @Parameter(name = "secretPhrase", in = ParameterIn.QUERY, required = true, description = "secret phrase in HEX")
    @Parameter(name = "host", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "weight", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer"))
    @Parameter(name = "date", in = ParameterIn.QUERY, required = true)
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        String host = req.getParameter("host");
        String weightValue = req.getParameter("weight");
        String dateValue = req.getParameter("date");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        } else if (host == null) {
            return MISSING_HOST;
        } else if (weightValue == null) {
            return MISSING_WEIGHT;
        } else if (dateValue == null) {
            return MISSING_DATE;
        }

        if (host.length() > 100) {
            return INCORRECT_HOST;
        }

        int weight;
        try {
            weight = Integer.parseInt(weightValue);
            if (weight <= 0 || weight > Constants.MAX_BALANCE_NXT) {
                return INCORRECT_WEIGHT;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_WEIGHT;
        }

        try {

            String hallmark = Hallmark.generateHallmark(secretPhrase, host, weight, Hallmark.parseDate(dateValue));

            JSONObject response = new JSONObject();
            response.put("hallmark", hallmark);
            return response;

        } catch (RuntimeException e) {
            return INCORRECT_DATE;
        }

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
