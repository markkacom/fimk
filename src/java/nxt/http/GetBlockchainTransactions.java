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
import nxt.Account;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

@Path("/fimk?requestType=getBlockchainTransactions")
public final class GetBlockchainTransactions extends APIServlet.APIRequestHandler {

    static final GetBlockchainTransactions instance = new GetBlockchainTransactions();

    private GetBlockchainTransactions() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TRANSACTIONS}, "account", "timestamp", "type", "subtype",
                "firstIndex", "lastIndex", "numberOfConfirmations", "withMessage", "phasedOnly", "nonPhasedOnly");
    }

    @Override
    @Operation(summary = "Get blockchain transactions",
            tags = {APITag2.ACCOUNT, APITag2.TRANSACTIONS})
    @Parameter(name = "account", in = ParameterIn.QUERY, description = "account id")
    @Parameter(name = "timestamp", in = ParameterIn.QUERY, schema = @Schema(type = "integer"))
    @Parameter(name = "type", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "transaction type")
    @Parameter(name = "subtype", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "transaction subtype")
    @Parameter(name = "firstIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "first index")
    @Parameter(name = "lastIndex", in = ParameterIn.QUERY, schema = @Schema(type = "integer"), description = "last index")
    @Parameter(name = "numberOfConfirmations", in = ParameterIn.QUERY, schema = @Schema(type = "integer"),
            description = "number of confirmations")
    @Parameter(name = "withMessage", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "with message")
    @Parameter(name = "phasedOnly", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "phased only")
    @Parameter(name = "nonPhasedOnly", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "non phased only")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account;
        if (Convert.emptyToNull(req.getParameter("account")) != null) {
            account = ParameterParser.getAccount(req);
        }
        else {
            account = null;
        }
        int timestamp = ParameterParser.getTimestamp(req);
        int numberOfConfirmations = ParameterParser.getNumberOfConfirmations(req);
        boolean withMessage = "true".equalsIgnoreCase(req.getParameter("withMessage"));
        boolean phasedOnly = "true".equalsIgnoreCase(req.getParameter("phasedOnly"));
        boolean nonPhasedOnly = "true".equalsIgnoreCase(req.getParameter("nonPhasedOnly"));

        byte type;
        byte subtype;
        try {
            type = Byte.parseByte(req.getParameter("type"));
        } catch (NumberFormatException e) {
            type = -1;
        }
        try {
            subtype = Byte.parseByte(req.getParameter("subtype"));
        } catch (NumberFormatException e) {
            subtype = -1;
        }

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        if (account == null) {
            lastIndex = Math.min(firstIndex + 20, lastIndex);
        }

        JSONArray transactions = new JSONArray();
        try (DbIterator<? extends Transaction> iterator = account != null ?
                Nxt.getBlockchain().getTransactions(account, numberOfConfirmations, type, subtype, timestamp,  withMessage, phasedOnly, nonPhasedOnly, firstIndex, lastIndex) :
                Nxt.getBlockchain().getTransactions(numberOfConfirmations, type, subtype, timestamp,  withMessage, phasedOnly, nonPhasedOnly, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                JSONObject json = JSONData.transaction(transaction);
                //json.remove("signature");
                //json.remove("signatureHash");
                transactions.add(json);
            }
        }

        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;

    }

}
