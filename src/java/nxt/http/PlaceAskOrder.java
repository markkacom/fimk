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
import nxt.*;
import nxt.txn.AskOrderPlacementAttachment;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.NOT_ENOUGH_ASSETS;

@Path("/fimk?requestType=placeAskOrder")
public final class PlaceAskOrder extends CreateTransaction {

    static final PlaceAskOrder instance = new PlaceAskOrder();

    private PlaceAskOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "quantityQNT", "priceNQT", "orderFeeQNT");
    }

    @SuppressWarnings("unchecked")
    @Override
    @Operation(summary = "Place ask order",
            tags = {APITag2.AE, APITag2.CREATE_TRANSACTION})
    @Parameter(name = "asset", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "asset id")
    @Parameter(name = "quantityQNT", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "quantity in QNT")
    @Parameter(name = "priceNQT", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "price in NQT")
    @Parameter(name = "orderFeeQNT", in = ParameterIn.QUERY, schema = @Schema(type = "integer", minimum = "0"),
            description = "order fee in QNT")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Asset asset = ParameterParser.getAsset(req);
        long priceNQT = ParameterParser.getPriceNQT(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        long orderFeeQNT = ParameterParser.getOrderFeeQNT(req, asset.getQuantityQNT());
        Account account = ParameterParser.getSenderAccount(req);

        long assetBalance = account.getUnconfirmedAssetBalanceQNT(asset.getId());
        if (assetBalance < 0 || quantityQNT > assetBalance) {
            return NOT_ENOUGH_ASSETS;
        }

        if (MofoAsset.isPrivateAsset(asset)) {
            long minOrderFeeQNT = MofoAsset.calculateOrderFee(asset.getId(), quantityQNT);
            if (minOrderFeeQNT > orderFeeQNT) {
                JSONObject response = new JSONObject();
                response.put("error", "Insufficient \"orderFeeQNT\": minimum of " + Long.valueOf(minOrderFeeQNT) + " required");
                return JSON.prepare(response);
            }
        }

        Attachment attachment = new AskOrderPlacementAttachment(asset.getId(), quantityQNT, priceNQT, orderFeeQNT);
        return createTransaction(req, account, attachment);
    }
}
