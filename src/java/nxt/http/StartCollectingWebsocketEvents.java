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

import nxt.NxtException;
import nxt.http.websocket.MofoSocketServer;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class StartCollectingWebsocketEvents extends APIServlet.APIRequestHandler {

    static final StartCollectingWebsocketEvents instance = new StartCollectingWebsocketEvents();

    private StartCollectingWebsocketEvents() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        MofoSocketServer.startCollectingEvents(); 
        JSONObject response = new JSONObject();
        return response;
    }

}
