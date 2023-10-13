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

import java.util.Iterator;

import nxt.NxtException;
import nxt.http.websocket.MofoSocketServer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetWebsocketEvents extends APIServlet.APIRequestHandler {

    static final GetWebsocketEvents instance = new GetWebsocketEvents();

    private GetWebsocketEvents() {
        super(new APITag[] {APITag.DEBUG});
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
      
        String[] topics = null;
        String topic = req.getParameter("topic");
        boolean all = "*".equals(topic);
        if (topic.contains(",")) {
            topics = topic.split(",");
        }
      
        JSONArray json = new JSONArray();
        if (MofoSocketServer.debugEvents != null) {
            Iterator<JSONArray> iterator = MofoSocketServer.debugEvents.iterator();
            while (iterator.hasNext()) {
                JSONArray event = iterator.next();
                if (all) {
                    json.add(event);
                }
                else if (topics != null) {
                    for (String t : topics) {
                        if (((String)event.get(1)).startsWith(t)) {
                            json.add(event);
                        }
                    }
                }
                else if (((String)event.get(1)).startsWith(topic)) {
                    json.add(event);
                }
            }
        }
  
        JSONObject response = new JSONObject();
        response.put("events", json);
        return response;
    }

}
