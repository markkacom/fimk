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
import nxt.TaggedData;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=searchTaggedData")
public final class SearchTaggedData extends APIServlet.APIRequestHandler {

    static final SearchTaggedData instance = new SearchTaggedData();

    private SearchTaggedData() {
        super(new APITag[] {APITag.DATA, APITag.SEARCH}, "query", "tag", "channel", "account", "firstIndex", "lastIndex", "includeData");
    }

    @Override
    @Operation(summary = "Search tagged data",
            tags = {APITag2.DATA, APITag2.SEARCH})
    @Parameter(name = "query", in = ParameterIn.QUERY)
    @Parameter(name = "tag", in = ParameterIn.QUERY)
    @Parameter(name = "channel", in = ParameterIn.QUERY)
    @Parameter(name = "account", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"),
            description = "account id")
    @Parameter(name = "includeData", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "include data")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long accountId = ParameterParser.getAccountId(req, "account", false);
        String query = ParameterParser.getSearchQuery(req);
        String channel = Convert.emptyToNull(req.getParameter("channel"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeData = !"false".equalsIgnoreCase(req.getParameter("includeData"));

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("data", jsonArray);
        try (DbIterator<TaggedData> data = TaggedData.searchData(query, channel, accountId, firstIndex, lastIndex)) {
            while (data.hasNext()) {
                jsonArray.add(JSONData.taggedData(data.next(), includeData));
            }
        }

        return response;
    }

}
