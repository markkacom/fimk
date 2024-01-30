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
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.*;

@Path("/fimk?requestType=dgsDelivery")
public final class DGSDelivery extends CreateTransaction {

    static final DGSDelivery instance = new DGSDelivery();

    private DGSDelivery() {
        super(new APITag[] {APITag.DGS, APITag.CREATE_TRANSACTION},
                "purchase", "discountNQT", "goodsToEncrypt", "goodsIsText", "goodsData", "goodsNonce");
    }

    @Override
    @Operation(summary = "Register delivery of market item",
            tags = {APITag2.DGS, APITag2.CREATE_TRANSACTION})
    @Parameter(name = "purchase", in = ParameterIn.QUERY, required = true, description = "purchase id")
    @Parameter(name = "discountNQT", in = ParameterIn.QUERY, description = "discount in NQT")
    @Parameter(name = "goodsToEncrypt", in = ParameterIn.QUERY, description = "goods to encrypt")
    @Parameter(name = "goodsIsText", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "is goods to encrypt data in text format or in HEX format")
    @Parameter(name = "goodsData", in = ParameterIn.QUERY, description = "goods data in HEX format")
    @Parameter(name = "goodsNonce", in = ParameterIn.QUERY, description = "goods data nonce in HEX format")
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {

        Account sellerAccount = ParameterParser.getSenderAccount(req);
        DigitalGoodsStore.Purchase purchase = ParameterParser.getPurchase(req);
        if (sellerAccount.getId() != purchase.getSellerId()) {
            return INCORRECT_PURCHASE;
        }
        if (! purchase.isPending()) {
            return ALREADY_DELIVERED;
        }

        String discountValueNQT = Convert.emptyToNull(req.getParameter("discountNQT"));
        long discountNQT = 0;
        try {
            if (discountValueNQT != null) {
                discountNQT = Long.parseLong(discountValueNQT);
            }
        } catch (RuntimeException e) {
            return INCORRECT_DGS_DISCOUNT;
        }
        if (discountNQT < 0
                || discountNQT > Constants.MAX_BALANCE_NQT
                || discountNQT > Math.multiplyExact(purchase.getPriceNQT(), (long) purchase.getQuantity())) {
            return INCORRECT_DGS_DISCOUNT;
        }

        Account buyerAccount = Account.getAccount(purchase.getBuyerId());
        boolean goodsIsText = !"false".equalsIgnoreCase(req.getParameter("goodsIsText"));
        EncryptedData encryptedGoods = ParameterParser.getEncryptedGoods(req);

        if (encryptedGoods == null) {
            String secretPhrase = ParameterParser.getSecretPhrase(req);
            byte[] goodsBytes;
            try {
                String plainGoods = Convert.nullToEmpty(req.getParameter("goodsToEncrypt"));
                if (plainGoods.length() == 0) {
                    return INCORRECT_DGS_GOODS;
                }
                goodsBytes = goodsIsText ? Convert.toBytes(plainGoods) : Convert.parseHexString(plainGoods);
            } catch (RuntimeException e) {
                return INCORRECT_DGS_GOODS;
            }
            encryptedGoods = buyerAccount.encryptTo(goodsBytes, secretPhrase, true);
        }

        Attachment attachment = new Attachment.DigitalGoodsDelivery(purchase.getId(), encryptedGoods, goodsIsText, discountNQT);
        return createTransaction(req, sellerAccount, buyerAccount.getId(), 0, attachment);

    }

}
