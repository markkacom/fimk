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
import nxt.Block;
import nxt.Nxt;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

@Path("/fimk?requestType=popOff")
public final class PopOff extends AdminAPIRequestHandler {

    static final PopOff instance = new PopOff();

    private PopOff() {
        super(new APITag[] {APITag.DEBUG}, "numBlocks", "height");
    }

    @Override
    @POST
    @Operation(summary = "Pop off blocks", description = "Specify at least one of parameters 'numBlocks', 'height'",
            tags = {APITag2.DEBUG})
    @Parameter(name = "numBlocks", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"))
    @Parameter(name = "height", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"))
    public JSONStreamAware processRequest(HttpServletRequest req) {

        int numBlocks = 0;
        try {
            numBlocks = Integer.parseInt(req.getParameter("numBlocks"));
        } catch (NumberFormatException ignored) {}
        int height = 0;
        try {
            height = Integer.parseInt(req.getParameter("height"));
        } catch (NumberFormatException ignored) {}

        List<? extends Block> blocks;
        try {
            Nxt.getBlockchainProcessor().setGetMoreBlocks(false);
            if (numBlocks > 0) {
                blocks = Nxt.getBlockchainProcessor().popOffTo(Nxt.getBlockchain().getHeight() - numBlocks);
            } else if (height > 0) {
                blocks = Nxt.getBlockchainProcessor().popOffTo(height);
            } else {
                return JSONResponses.missing("numBlocks", "height");
            }
        } finally {
            Nxt.getBlockchainProcessor().setGetMoreBlocks(true);
        }
        JSONArray blocksJSON = new JSONArray();
        blocks.forEach(block -> blocksJSON.add(JSONData.block(block, true)));
        JSONObject response = new JSONObject();
        response.put("blocks", blocksJSON);
        return response;
    }

    @Override
    final boolean requirePost() {
        return true;
    }

}
