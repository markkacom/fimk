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
import nxt.PhasingPoll;
import nxt.PhasingVote;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getPhasingPollVotes")
public class GetPhasingPollVotes extends APIServlet.APIRequestHandler  {
    static final GetPhasingPollVotes instance = new GetPhasingPollVotes();

    private GetPhasingPollVotes() {
        super(new APITag[] {APITag.PHASING}, "transaction", "firstIndex", "lastIndex");
    }

    @Override
    @Operation(summary = "Get phasing poll votes",
            tags = {APITag2.PHASING})
    @Parameter(name = "transaction", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "transaction id")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        PhasingPoll phasingPoll = PhasingPoll.getPoll(transactionId);
        if (phasingPoll != null) {
            JSONObject response = new JSONObject();
            JSONArray votesJSON = new JSONArray();
            try (DbIterator<PhasingVote> votes = PhasingVote.getVotes(transactionId, firstIndex, lastIndex)) {
                for (PhasingVote vote : votes) {
                    votesJSON.add(JSONData.phasingPollVote(vote));
                }
            }
            response.put("votes", votesJSON);
            return response;
        }
        return JSONResponses.UNKNOWN_TRANSACTION;
    }
}
