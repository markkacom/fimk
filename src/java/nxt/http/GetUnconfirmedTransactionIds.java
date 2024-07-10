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
import nxt.Transaction;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;

@Path("/fimk?requestType=getUnconfirmedTransactionIds")
public final class GetUnconfirmedTransactionIds extends APIServlet.APIRequestHandler {

    static final GetUnconfirmedTransactionIds instance = new GetUnconfirmedTransactionIds();

    private GetUnconfirmedTransactionIds() {
        super(new APITag[] {APITag.TRANSACTIONS, APITag.ACCOUNTS}, "account");
    }

    @Override
    @Operation(summary = "Get unconfirmed transaction identifiers",
            tags = {APITag2.TRANSACTIONS, APITag2.ACCOUNT})
    @Parameter(name = "account", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"),
            description = "account id")
    public JSONStreamAware processRequest(HttpServletRequest req) {

        String accountIdString = Convert.emptyToNull(req.getParameter("account"));
        long accountId = 0;

        if (accountIdString != null) {
            try {
                accountId = Convert.parseAccountId(accountIdString);
            } catch (RuntimeException e) {
                return INCORRECT_ACCOUNT;
            }
        }

        JSONArray transactionIds = new JSONArray();
        try (DbIterator<? extends Transaction> transactionsIterator = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
            while (transactionsIterator.hasNext()) {
                Transaction transaction = transactionsIterator.next();
                if (accountId != 0 && !(accountId == transaction.getSenderId() || accountId == transaction.getRecipientId())) {
                    continue;
                }
                transactionIds.add(transaction.getStringId());
            }
        }

        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactionIds", transactionIds);
        return response;
    }

}
