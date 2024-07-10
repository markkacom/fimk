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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import nxt.Nxt;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/fimk?requestType=scan")
public final class Scan extends AdminAPIRequestHandler {

    static final Scan instance = new Scan();

    private Scan() {
        super(new APITag[] {APITag.DEBUG}, "numBlocks", "height", "validate");
    }

    @Override
    @POST
    @Operation(summary = "Run blockchain scanning",
            tags = {APITag2.DEBUG})
    @Parameter(name = "numBlocks", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "number of blocks")
    @Parameter(name = "height", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"))
    @Parameter(name = "validate", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "validate")
    public JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try {
            boolean validate = "true".equalsIgnoreCase(req.getParameter("validate"));
            int numBlocks = 0;
            try {
                numBlocks = Integer.parseInt(req.getParameter("numBlocks"));
            } catch (NumberFormatException ignored) {}
            int height = -1;
            try {
                height = Integer.parseInt(req.getParameter("height"));
            } catch (NumberFormatException ignore) {}
            long start = System.currentTimeMillis();
            try {
                Nxt.getBlockchainProcessor().setGetMoreBlocks(false);
                if (numBlocks > 0) {
                    Nxt.getBlockchainProcessor().scan(Nxt.getBlockchain().getHeight() - numBlocks + 1, validate);
                } else if (height >= 0) {
                    Nxt.getBlockchainProcessor().scan(height, validate);
                } else {
                    return JSONResponses.missing("numBlocks", "height");
                }
            } finally {
                Nxt.getBlockchainProcessor().setGetMoreBlocks(true);
            }
            long end = System.currentTimeMillis();
            response.put("done", true);
            response.put("scanTime", (end - start)/1000);
        } catch (RuntimeException e) {
            JSONData.putException(response, e);
        }
        return response;
    }

    @Override
    final boolean requirePost() {
        return true;
    }

}
