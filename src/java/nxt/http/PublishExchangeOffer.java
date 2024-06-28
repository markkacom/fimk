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


/**
 * Publish exchange offer for {@link nxt.CurrencyType#EXCHANGEABLE} currency
 * <p>
 * Parameters
 * <ul>
 * <li>currency - currency id of an active currency
 * <li>buyRateNQT - NXT amount for buying a currency unit specified in NQT
 * <li>sellRateNQT - NXT amount for selling a currency unit specified in NQT
 * <li>initialBuySupply - Initial number of currency units offered to buy by the publisher
 * <li>initialSellSupply - Initial number of currency units offered for sell by the publisher
 * <li>totalBuyLimit - Total number of currency units which can be bought from the offer
 * <li>totalSellLimit - Total number of currency units which can be sold from the offer
 * <li>expirationHeight - Blockchain height at which the offer is expired
 * </ul>
 *
 * <p>
 * Publishing an exchange offer internally creates a buy offer and a counter sell offer linked together.
 * Typically the buyRateNQT specified would be less than the sellRateNQT thus allowing the publisher to make profit
 *
 * <p>
 * Each {@link CurrencyBuy} transaction which matches this offer reduces the sell supply and increases the buy supply
 * Similarly, each {@link CurrencySell} transaction which matches this offer reduces the buy supply and increases the sell supply
 * Therefore the multiple buy/sell transaction can be issued against this offer during it's lifetime.
 * However, the total buy limit and sell limit stops exchanging based on this offer after the accumulated buy/sell limit is reached
 * after possibly multiple exchange operations.
 *
 * <p>
 * Only one exchange offer is allowed per account. Publishing a new exchange offer when another exchange offer exists
 * for the account, removes the existing exchange offer and publishes the new exchange offer
 */
@Path("/fimk?requestType=publishExchangeOffer")
public final class PublishExchangeOffer extends CreateTransaction {

    static final PublishExchangeOffer instance = new PublishExchangeOffer();

    private PublishExchangeOffer() {
        super(new APITag[] {APITag.MS, APITag.CREATE_TRANSACTION}, "currency", "buyRateNQT", "sellRateNQT",
                "totalBuyLimit", "totalSellLimit", "initialBuySupply", "initialSellSupply", "expirationHeight");
    }

    @Override
    @Operation(summary = "Publish exchange offer for currency",
            tags = {APITag2.MS, APITag2.CREATE_TRANSACTION},
    description = "Publishing an exchange offer internally creates a buy offer and a counter sell offer linked together." +
            " Typically the buyRateNQT specified would be less than the sellRateNQT thus allowing the publisher to make profit" +
            " Each CurrencyBuy transaction which matches this offer reduces the sell supply and increases the buy supply" +
            " Similarly, each CurrencySell transaction which matches this offer reduces the buy supply and increases the sell supply" +
            " Therefore the multiple buy/sell transaction can be issued against this offer during it's lifetime." +
            " However, the total buy limit and sell limit stops exchanging based on this offer after the accumulated buy/sell limit is reached" +
            " after possibly multiple exchange operations." +
            " Only one exchange offer is allowed per account. Publishing a new exchange offer when another exchange offer exists" +
            " for the account, removes the existing exchange offer and publishes the new exchange offer")
    @Parameter(name = "currency", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "currency id")
    @Parameter(name = "buyRateNQT", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "FIM amount for buying a currency unit specified in NQT")
    @Parameter(name = "sellRateNQT", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "FIM amount for selling a currency unit specified in NQT")
    @Parameter(name = "totalBuyLimit", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "total number of currency units which can be bought from the offer")
    @Parameter(name = "totalSellLimit", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "total number of currency units which can be sold from the offer")
    @Parameter(name = "initialBuySupply", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "initial number of currency units offered to buy by the publisher")
    @Parameter(name = "initialSellSupply", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "initial number of currency units offered for sell by the publisher")
    @Parameter(name = "expirationHeight", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "blockchain height at which the offer is expired")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        Currency currency = ParameterParser.getCurrency(req);
        long buyRateNQT = ParameterParser.getLong(req, "buyRateNQT", 0, Long.MAX_VALUE, true);
        long sellRateNQT= ParameterParser.getLong(req, "sellRateNQT", 0, Long.MAX_VALUE, true);
        long totalBuyLimit = ParameterParser.getLong(req, "totalBuyLimit", 0, Long.MAX_VALUE, true);
        long totalSellLimit = ParameterParser.getLong(req, "totalSellLimit", 0, Long.MAX_VALUE, true);
        long initialBuySupply = ParameterParser.getLong(req, "initialBuySupply", 0, Long.MAX_VALUE, true);
        long initialSellSupply = ParameterParser.getLong(req, "initialSellSupply", 0, Long.MAX_VALUE, true);
        int expirationHeight = ParameterParser.getInt(req, "expirationHeight", 0, Integer.MAX_VALUE, true);
        Account account = ParameterParser.getSenderAccount(req);

        Attachment attachment = new Attachment.MonetarySystemPublishExchangeOffer(currency.getId(), buyRateNQT, sellRateNQT,
                totalBuyLimit, totalSellLimit, initialBuySupply, initialSellSupply, expirationHeight);
        return createTransaction(req, account, attachment);
    }

}
