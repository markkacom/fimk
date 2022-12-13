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

import nxt.*;
import nxt.txn.BidOrderPlacementAttachment;
import nxt.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class PlaceBidOrder extends CreateTransaction {

    static final PlaceBidOrder instance = new PlaceBidOrder();

    private PlaceBidOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "quantityQNT", "priceNQT", "orderFeeNQT");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Asset asset = ParameterParser.getAsset(req);
        long priceNQT = ParameterParser.getPriceNQT(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        long feeNQT = ParameterParser.getFeeNQT(req);
        long orderFeeNQT = ParameterParser.getOrderFeeNQT(req);
        Account account = ParameterParser.getSenderAccount(req);

        long totalNQT = Math.multiplyExact(priceNQT, quantityQNT);

        try {
            if (Math.addExact(feeNQT, totalNQT) > account.getUnconfirmedBalanceNQT()) {
                return NOT_ENOUGH_FUNDS;
            }
        } catch (ArithmeticException e) {
            return NOT_ENOUGH_FUNDS;
        }

        if (MofoAsset.isPrivateAsset(asset)) {
            long minOrderFeeNQT = MofoAsset.calculateOrderFee(asset.getId(), totalNQT);
            if (minOrderFeeNQT > orderFeeNQT) {
                JSONObject response = new JSONObject();
                response.put("error", "Insufficient \"orderFeeNQT\": minimum of " + Long.valueOf(minOrderFeeNQT) + " required");
                return JSON.prepare(response);
            }
        }

        Attachment attachment = new BidOrderPlacementAttachment(asset.getId(), quantityQNT, priceNQT, orderFeeNQT);
        return createTransaction(req, account, attachment);
    }

}
