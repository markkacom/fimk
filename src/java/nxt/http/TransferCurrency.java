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
import nxt.Attachment;
import nxt.Currency;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.NOT_ENOUGH_CURRENCY;

@Path("/fimk?requestType=transferCurrency")
public final class TransferCurrency extends CreateTransaction {

    static final TransferCurrency instance = new TransferCurrency();

    private TransferCurrency() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "recipient", "currency", "units");
    }

    @Override
    @Operation(summary = "Transfer currency",
            tags = {APITag2.MS, APITag2.CREATE_TRANSACTION})
    @Parameter(name = "recipient", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "recipient account id")
    @Parameter(name = "currency", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "currency id")
    @Parameter(name = "units", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"))
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long recipient = ParameterParser.getAccountId(req, "recipient", true);

        Currency currency = ParameterParser.getCurrency(req);
        long units = ParameterParser.getLong(req, "units", 0, Long.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);

        if (units > account.getUnconfirmedCurrencyUnits(currency.getId())) {
            return NOT_ENOUGH_CURRENCY;
        }

        Attachment attachment = new Attachment.MonetarySystemCurrencyTransfer(currency.getId(), units);
        return createTransaction(req, account, recipient, 0, attachment);
    }

}
