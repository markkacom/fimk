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

import nxt.http.EventListener.EventListenerException;
import nxt.http.EventListener.PendingEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>The EventWait API will wait for one of the server events
 * registered by EventRegister.  EventWait will return immediately
 * if one or more events have occurred since the last time EventWait
 * was called.  All pending events will be returned in a single response.
 * The events remain registered so successive calls to EventWait can
 * be made without another call to EventRegister.</p>
 *
 * <p>Only one EventWait can be outstanding for the same network address.
 * If a second EventWait is issued, the current EventWait will be replaced
 * by the new EventWait.</p>
 *
 * <p>Request parameters:</p>
 * <ul>
 * <li>timeout - Number of seconds to wait for an event.  The EventWait
 * will complete normally if no event is received within the timeout interval.
 * fimk.apiEventTimeout will be used if no timeout value is specified or
 * if the requested timeout is greater than fimk.apiEventTimeout.</li>
 * </ul>
 *
 * <p>Response parameters:</p>
 * <ul>
 * <li>events - An array of event objects</li>
 * </ul>
 *
 * <p>Error Response parameters:</p>
 * <ul>
 * <li>errorCode - API error code</li>
 * <li>errorDescription - API error description</li>
 * </ul>
 *
 * <p>Event object:</p>
 * <ul>
 * <li>name - The event name</li>
 * <li>ids - An array of event object identifiers</li>
 * </ul>
 *
 * <p>Event names:</p>
 * <ul>
 * <li>Block.BLOCK_GENERATED</li>
 * <li>Block.BLOCK_POPPED</li>
 * <li>Block.BLOCK_PUSHED</li>
 * <li>Peer.ADD_INBOUND</li>
 * <li>Peer.ADDED_ACTIVE_PEER</li>
 * <li>Peer.BLACKLIST</li>
 * <li>Peer.CHANGED_ACTIVE_PEER</li>
 * <li>Peer.DEACTIVATE</li>
 * <li>Peer.NEW_PEER</li>
 * <li>Peer.REMOVE</li>
 * <li>Peer.REMOVE_INBOUND</li>
 * <li>Peer.UNBLACKLIST</li>
 * <li>Transaction.ADDED_CONFIRMED_TRANSACTIONS</li>
 * <li>Transaction.ADDED_UNCONFIRMED_TRANSACTIONS</li>
 * <li>Transaction.REJECT_PHASED_TRANSACTION</li>
 * <li>Transaction.RELEASE_PHASED_TRANSACTION</li>
 * <li>Transaction.REMOVE_UNCONFIRMED_TRANSACTIONS</li>
 * </ul>
 *
 * <p>Event object identifiers:</p>
 * <ul>
 * <li>Block string identifier for a Block event</li>
 * <li>Peer network address for a Peer event</li>
 * <li>Transaction string identifier for a Transaction event</li>
 * </ul>
 */
public class EventWait extends APIServlet.APIRequestHandler {

    /** EventWait instance */
    static final EventWait instance = new EventWait();

    /** Incorrect timeout */
    private static final JSONObject incorrectTimeout = new JSONObject();
    static {
        incorrectTimeout.put("errorCode", 4);
        incorrectTimeout.put("errorDescription", "Wait timeout is not valid");
    }

    /** No events registered */
    private static final JSONObject noEventsRegistered = new JSONObject();
    static {
        noEventsRegistered.put("errorCode", 8);
        noEventsRegistered.put("errorDescription", "No events registered");
    }

    /**
     * Create the EventWait instance
     */
    private EventWait() {
        super(new APITag[] {APITag.INFO}, "timeout");
    }

    /**
     * Process the EventWait API request
     *
     * The response will be returned immediately if there are any
     * pending events.  Otherwise, an asynchronous context will
     * be created and the response will be returned after the wait
     * has completed.  By using an asynchronous context, we avoid
     * tying up the Jetty servlet thread while waiting for an event.
     *
     * @param   req                 API request
     * @return                      API response or null
     */
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = null;
        //
        // Get the timeout value
        //
        long timeout = EventListener.eventTimeout;
        String value = req.getParameter("timeout");
        if (value != null) {
            try {
                timeout = Math.min(Long.valueOf(value), timeout);
            } catch (NumberFormatException exc) {
                response = incorrectTimeout;
            }
        }
        //
        // Wait for an event
        //
        if (response == null) {
            EventListener listener = EventListener.eventListeners.get(req.getRemoteAddr());
            if (listener == null) {
                response = noEventsRegistered;
            } else {
                try {
                    List<PendingEvent> events = listener.eventWait(req, timeout);
                    if (events != null)
                        response = formatResponse(events);
                } catch (EventListenerException exc) {
                    response = new JSONObject();
                    response.put("errorCode", 7);
                    response.put("errorDescription", "Unable to wait for an event: "+exc.getMessage());
                }
            }
        }
        return response;
    }

    @Override
    final boolean requirePost() {
        return true;
    }

    /**
     * Format the EventWait response
     *
     * @param   events              Event list
     * @return                      JSON stream
     */
    static JSONObject formatResponse(List<PendingEvent> events) {
        JSONArray eventsJSON = new JSONArray();
        events.forEach(event -> {
            JSONArray idsJSON = new JSONArray();
            idsJSON.addAll(event.getIdList());
            JSONObject eventJSON = new JSONObject();
            eventJSON.put("name", event.getName());
            eventJSON.put("ids", idsJSON);
            eventsJSON.add(eventJSON);
        });
        JSONObject response = new JSONObject();
        response.put("events", eventsJSON);
        return response;
    }
}
