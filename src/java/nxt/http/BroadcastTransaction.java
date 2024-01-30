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
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * The purpose of broadcast transaction is to support client side signing of transactions.
 * Clients first submit their transaction using {@link nxt.http.CreateTransaction} without providing the secret phrase.<br>
 * In response the client receives the unsigned transaction JSON and transaction bytes.
 * <p>
 * The client then signs and submits the signed transaction using {@link nxt.http.BroadcastTransaction}
 * <p>
 * The default wallet implements this procedure in nrs.server.js which you can use as reference.
 * <p>
 * {@link nxt.http.BroadcastTransaction} accepts the following parameters:<br>
 * transactionJSON - JSON representation of the signed transaction<br>
 * transactionBytes - row bytes composing the signed transaction bytes excluding the prunable appendages<br>
 * prunableAttachmentJSON - JSON representation of the prunable appendages<br>
 * <p>
 * Clients can submit either the signed transactionJSON or the signed transactionBytes but not both.<br>
 * In case the client submits transactionBytes for a transaction containing prunable appendages, the client also needs
 * to submit the prunableAttachmentJSON parameter which includes the attachment JSON for the prunable appendages.<br>
 * <p>
 * Prunable appendages are classes implementing the {@link nxt.Appendix.Prunable} interface.
 */
@Path("/fimk?requestType=broadcastTransaction")
public final class BroadcastTransaction extends APIServlet.APIRequestHandler {

    static final BroadcastTransaction instance = new BroadcastTransaction();

    private BroadcastTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transactionJSON", "transactionBytes", "prunableAttachmentJSON");
    }

    @Override
    @POST
    @Operation(summary = "Broadcast transaction",
            tags = {APITag2.TRANSACTIONS},
            description = "The purpose of broadcast transaction is to support client side signing of transactions. " +
                    "Clients first submit their transaction using request CreateTransaction without providing the secret phrase. " +
                    "In response the client receives the unsigned transaction JSON and transaction bytes. " +
                    "The client then signs and submits the signed transaction using this request.")
    @Parameter(name = "transactionJSON", in = ParameterIn.QUERY, description = "JSON representation of the signed transaction")
    @Parameter(name = "transactionBytes", in = ParameterIn.QUERY, description = "row bytes composing the signed transaction bytes excluding the prunable appendages")
    @Parameter(name = "prunableAttachmentJSON", in = ParameterIn.QUERY, description = "JSON representation of the prunable appendages")
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));

        Transaction.Builder builder = ParameterParser.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON);
        Transaction transaction = builder.build();

        JSONObject response = new JSONObject();
        try {
            Nxt.getTransactionProcessor().broadcast(transaction);
            response.put("transaction", transaction.getStringId());
            response.put("fullHash", transaction.getFullHash());
        } catch (NxtException.ValidationException|RuntimeException e) {
            Logger.logDebugMessage(e.getMessage(), e);
            JSONData.putException(response, e);
        }
        return response;

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
