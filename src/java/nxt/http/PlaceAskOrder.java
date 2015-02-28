package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.MofoAsset;
import nxt.MofoAsset.PrivateAsset;
import nxt.util.Convert;
import nxt.NxtException;

import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.NOT_ENOUGH_ASSETS;
import static nxt.http.JSONResponses.ERROR_INCORRECT_REQUEST;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

public final class PlaceAskOrder extends CreateTransaction {

    static final PlaceAskOrder instance = new PlaceAskOrder();

    private PlaceAskOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "quantityQNT", "priceNQT", "orderFeeQNT");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Asset asset = ParameterParser.getAsset(req);
        long priceNQT = ParameterParser.getPriceNQT(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        long orderFeeQNT = ParameterParser.getOrderFeeQNT(req, asset.getQuantityQNT());
        Account account = ParameterParser.getSenderAccount(req);

        long assetBalance = account.getUnconfirmedAssetBalanceQNT(asset.getId());
        if (assetBalance < 0 || quantityQNT > assetBalance) {
            return NOT_ENOUGH_ASSETS;
        }

        PrivateAsset privateAsset = MofoAsset.getPrivateAsset(asset.getId());
        if (privateAsset != null) {
            try {
                if (assetBalance < Convert.safeAdd(quantityQNT, orderFeeQNT)) {
                    return NOT_ENOUGH_ASSETS;
                }
            } catch (ArithmeticException e) {
                return NOT_ENOUGH_ASSETS;
            }
            if (privateAsset.calculateOrderFee(quantityQNT) > orderFeeQNT) {
                return ERROR_INCORRECT_REQUEST;
            }
        }

        Attachment attachment = new Attachment.ColoredCoinsAskOrderPlacement(asset.getId(), quantityQNT, priceNQT, orderFeeQNT);
        return createTransaction(req, account, attachment);
    }
}
