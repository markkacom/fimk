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
import nxt.NxtException;
import nxt.Poll;
import nxt.Vote;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getPollVotes")
public class GetPollVotes extends APIServlet.APIRequestHandler  {
    static final GetPollVotes instance = new GetPollVotes();

    private GetPollVotes() {
        super(new APITag[] {APITag.VS}, "poll", "firstIndex", "lastIndex");
    }

    @Override
    @Operation(summary = "Get poll votes",
            tags = {APITag2.VS})
    @Parameter(name = "poll", in = ParameterIn.QUERY, required = true, description = "poll id")
    @Parameter(name = "account", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "account id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        Poll poll = ParameterParser.getPoll(req);

        JSONArray votesJson = new JSONArray();
        try (DbIterator<Vote> votes = Vote.getVotes(poll.getId(), firstIndex, lastIndex)) {
            for (Vote vote : votes) {
                votesJson.add(JSONData.vote(vote));
            }
        }
        JSONObject response = new JSONObject();
        response.put("votes", votesJson);
        return response;
    }
}
