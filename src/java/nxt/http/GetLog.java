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
import nxt.util.MemoryHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * <p>The GetLog API will return log messages from the ring buffer
 * maintained by the MemoryHandler log handler.  The most recent
 * 'count' messages will be returned.  All log messages in the
 * ring buffer will be returned if 'count' is omitted.</p>
 *
 * <p>Request parameters:</p>
 * <ul>
 * <li>count - The number of log messages to return</li>
 * </ul>
 *
 * <p>Response parameters:</p>
 * <ul>
 * <li>messages - An array of log messages</li>
 * </ul>
 */
@Path("/fimk?requestType=getLog")
public final class GetLog extends AdminAPIRequestHandler {

    /** GetLog instance */
    static final GetLog instance = new GetLog();

    /**
     * Create the GetLog instance
     */
    private GetLog() {
        super(new APITag[] {APITag.DEBUG}, "count");
    }

    /**
     * Process the GetLog API request
     *
     * @param   req                 API request
     * @return                      API response
     */
    @Override
    @Operation(summary = "Get log",
            description = "Return log messages from the ring buffer maintained by the MemoryHandler log handler.  The most recent 'count' messages will be returned.  All log messages in the ring buffer will be returned if 'count' is omitted",
            tags = {APITag2.DEBUG})
    @Parameter(name = "count", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "the number of log messages to return")
    public JSONStreamAware processRequest(HttpServletRequest req) {
        //
        // Get the number of log messages to return
        //
        int count;
        String value = req.getParameter("count");
        if (value != null)
            count = Math.max(Integer.valueOf(value), 0);
        else
            count = Integer.MAX_VALUE;
        //
        // Get the log messages
        //
        JSONArray logJSON = new JSONArray();
        Logger logger = Logger.getLogger("");
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof MemoryHandler) {
                logJSON.addAll(((MemoryHandler)handler).getMessages(count));
                break;
            }
        }
        //
        // Return the response
        //
        JSONObject response = new JSONObject();
        response.put("messages", logJSON);
        return response;
    }

}
