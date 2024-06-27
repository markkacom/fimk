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
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getPolls")
public class GetPolls extends APIServlet.APIRequestHandler {

    static final GetPolls instance = new GetPolls();

    private GetPolls() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.VS}, "account", "firstIndex", "lastIndex", "includeFinished");
    }

    @Override
    @Operation(summary = "Get polls",
            tags = {APITag2.ACCOUNT, APITag2.VS})
    @Parameter(name = "account", in = ParameterIn.QUERY, description = "account id")
    @Parameter(name = "includeFinished", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "include finished")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long accountId = ParameterParser.getAccountId(req, "account", false);
        boolean includeFinished = "true".equalsIgnoreCase(req.getParameter("includeFinished"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray pollsJson = new JSONArray();
        DbIterator<Poll> polls = null;
        try {
            if (accountId == 0) {
                if (includeFinished) {
                    polls = Poll.getAllPolls(firstIndex, lastIndex);
                } else {
                    polls = Poll.getActivePolls(firstIndex, lastIndex);
                }
            } else {
                polls = Poll.getPollsByAccount(accountId, includeFinished, firstIndex, lastIndex);
            }

            while (polls.hasNext()) {
                pollsJson.add(JSONData.poll(polls.next()));
            }
        } finally {
            DbUtils.close(polls);
        }

        JSONObject response = new JSONObject();
        response.put("polls", pollsJson);
        return response;
    }
}
