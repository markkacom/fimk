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
import nxt.VoteWeighting;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.List;

import static nxt.http.JSONResponses.POLL_RESULTS_NOT_AVAILABLE;

@Path("/fimk?requestType=getPollResult")
public class GetPollResult extends APIServlet.APIRequestHandler {

    static final GetPollResult instance = new GetPollResult();

    private GetPollResult() {
        super(new APITag[]{APITag.VS}, "poll", "votingModel", "holding", "minBalance", "minBalanceModel");
    }

    @Override
    @Operation(summary = "Get poll result",
            tags = {APITag2.VS})
    @Parameter(name = "poll", in = ParameterIn.QUERY, required = true, description = "poll id")
    @Parameter(name = "votingModel", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0", maximum = "3"),
            description = "voting model (min 0, max 3)")
    @Parameter(name = "holding", in = ParameterIn.QUERY, description = "holding id")
    @Parameter(name = "minBalance", in = ParameterIn.QUERY, description = "min balance")
    @Parameter(name = "maxBalance", in = ParameterIn.QUERY, description = "max balance")
    @Parameter(name = "minBalanceModel", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0", maximum = "3"),
            description = "min balance model (min 0, max 3)")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Poll poll = ParameterParser.getPoll(req);
        List<Poll.OptionResult> pollResults;
        VoteWeighting voteWeighting;
        if (Convert.emptyToNull(req.getParameter("votingModel")) == null) {
            pollResults = poll.getResults();
            voteWeighting = poll.getVoteWeighting();
        } else {
            byte votingModel = ParameterParser.getByte(req, "votingModel", (byte)0, (byte)3, true);
            long holdingId = ParameterParser.getLong(req, "holding", Long.MIN_VALUE, Long.MAX_VALUE, false);
            long minBalance = ParameterParser.getLong(req, "minBalance", 0, Long.MAX_VALUE, false);
            byte minBalanceModel = ParameterParser.getByte(req, "minBalanceModel", (byte)0, (byte)3, false);
            voteWeighting = new VoteWeighting(votingModel, holdingId, minBalance, minBalanceModel);
            voteWeighting.validate();
            pollResults = poll.getResults(voteWeighting);
        }
        if (pollResults == null) {
            return POLL_RESULTS_NOT_AVAILABLE;
        }
        return JSONData.pollResults(poll, pollResults, voteWeighting);
    }
}
