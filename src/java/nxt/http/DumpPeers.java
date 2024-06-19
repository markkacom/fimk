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
import nxt.Constants;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/fimk?requestType=dumpPeers")
public final class DumpPeers extends APIServlet.APIRequestHandler {

    static final DumpPeers instance = new DumpPeers();

    private DumpPeers() {
        super(new APITag[] {APITag.DEBUG}, "version", "weight");
    }

    @Override
    @GET
    @Operation(summary = "Dump peers",
            tags = {APITag2.DEBUG})
    @Parameter(name = "version", in = ParameterIn.QUERY, description = "version of peer")
    @Parameter(name = "weight", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "weight of peer")
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String version = Convert.nullToEmpty(req.getParameter("version"));
        int weight = ParameterParser.getInt(req, "weight", 0, (int)Constants.MAX_BALANCE_NXT, false);
        Set<String> addresses = Peers.getAllPeers().parallelStream().unordered()
                .filter(peer -> peer.getState() == Peer.State.CONNECTED
                        && peer.shareAddress()
                        && !peer.isBlacklisted()
                        && peer.getVersion() != null && peer.getVersion().startsWith(version)
                        && (weight == 0 || peer.getWeight() > weight))
                .map(Peer::getAnnouncedAddress)
                .collect(Collectors.toSet());
        StringBuilder buf = new StringBuilder();
        for (String address : addresses) {
            buf.append(address).append("; ");
        }
        JSONObject response = new JSONObject();
        response.put("peers", buf.toString());
        return response;
    }

}
