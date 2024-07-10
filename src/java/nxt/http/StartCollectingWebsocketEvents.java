/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import io.swagger.v3.oas.annotations.Operation;
import nxt.NxtException;
import nxt.http.websocket.MofoSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=startCollectingWebsocketEvents")
public final class StartCollectingWebsocketEvents extends APIServlet.APIRequestHandler {

    static final StartCollectingWebsocketEvents instance = new StartCollectingWebsocketEvents();

    private StartCollectingWebsocketEvents() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    @Operation(summary = "Start collecting websocket events",
            tags = {APITag2.DEBUG})
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        MofoSocketServer.startCollectingEvents(); 
        JSONObject response = new JSONObject();
        return response;
    }

}
