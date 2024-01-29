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
import nxt.NxtException;
import nxt.http.APIServlet.APIRequestHandler;
import nxt.peer.Peer;
import nxt.peer.Peers;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.MISSING_PEER;
import static nxt.http.JSONResponses.UNKNOWN_PEER;

@Path("/fimk?requestType=blacklistPeer")
public class BlacklistPeer extends APIRequestHandler {

    static final BlacklistPeer instance = new BlacklistPeer();
    
    private BlacklistPeer() {
        super(new APITag[] {APITag.NETWORK}, "peer");
    }

    @Override
    @POST
    @Operation(summary = "Manual blacklist peer",
            tags = {APITag2.NETWORK})
    @Parameter(name = "peer", in = ParameterIn.QUERY, required = true)
    public JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        JSONObject response = new JSONObject();
        
        String peerAddress = request.getParameter("peer");
        if (peerAddress == null) {
            return MISSING_PEER;
        }
        Peer peer = Peers.findOrCreatePeer(peerAddress, true);
        if (peer == null) return UNKNOWN_PEER;

        Peers.addPeer(peer);
        peer.blacklist("Manual blacklist");
        response.put("done", true);

        return response;
    }

    @Override
    final boolean requirePost() {
        return true;
    }

    @Override
    boolean requirePassword() {
        return true;
    }
}
