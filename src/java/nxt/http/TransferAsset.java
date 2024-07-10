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
import nxt.Asset;
import nxt.Attachment;
import nxt.NxtException;
import nxt.txn.AssetTransferAttachment;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;

import static nxt.http.JSONResponses.NOT_ENOUGH_ASSETS;

@Path("/fimk?requestType=transferAsset")
public final class TransferAsset extends CreateTransaction {

    static final TransferAsset instance = new TransferAsset();

    private TransferAsset() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "recipient", "asset", "quantityQNT");
    }

    @Override
    @Operation(summary = "Transfer asset",
            tags = {APITag2.AE, APITag2.CREATE_TRANSACTION})
    @Parameter(name = "recipient", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "recipient account id")
    @Parameter(name = "asset", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "asset id")
    @Parameter(name = "quantityQNT", in = ParameterIn.QUERY, required = true, schema = @Schema(type = "integer", minimum = "0"),
            description = "quantity in QNT")
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        long recipient = ParameterParser.getAccountId(req, "recipient", true);

        Asset asset = ParameterParser.getAsset(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        Account account = ParameterParser.getSenderAccount(req);

        long assetBalance = account.getUnconfirmedAssetBalanceQNT(asset.getId());
        if (assetBalance < 0 || quantityQNT > assetBalance) {
            return NOT_ENOUGH_ASSETS;
        }

        Attachment attachment = new AssetTransferAttachment(asset.getId(), quantityQNT);
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
