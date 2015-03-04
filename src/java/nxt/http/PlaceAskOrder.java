package nxt.http;

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.MofoAsset;
import nxt.util.Convert;
import nxt.util.JSON;
import nxt.NxtException;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.NOT_ENOUGH_ASSETS;

public final class PlaceAskOrder extends CreateTransaction {

    static final PlaceAskOrder instance = new PlaceAskOrder();

    private PlaceAskOrder() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "quantityQNT", "priceNQT", "orderFeeQNT");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Asset asset = ParameterParser.getAsset(req);
        long priceNQT = ParameterParser.getPriceNQT(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        long orderFeeQNT = Asset.privateEnabled() ? ParameterParser.getOrderFeeQNT(req, asset.getQuantityQNT()) : 0;
        Account account = ParameterParser.getSenderAccount(req);

        long assetBalance = account.getUnconfirmedAssetBalanceQNT(asset.getId());
        if (assetBalance < 0 || quantityQNT > assetBalance) {
            return NOT_ENOUGH_ASSETS;
        }

        if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(asset)) {
            try {
                if (assetBalance < Convert.safeAdd(quantityQNT, orderFeeQNT)) {
                    return NOT_ENOUGH_ASSETS;
                }
            } catch (ArithmeticException e) {
                return NOT_ENOUGH_ASSETS;
            }
            
            long minOrderFeeQNT = MofoAsset.calculateOrderFee(asset.getId(), quantityQNT);
            if (minOrderFeeQNT > orderFeeQNT) {
                JSONObject response = new JSONObject();
                response.put("error", "Insufficient \"orderFeeQNT\": minimum of " + Long.valueOf(minOrderFeeQNT) + " required");
                return JSON.prepare(response);
            }
        }

        Attachment attachment = new Attachment.ColoredCoinsAskOrderPlacement(asset.getId(), quantityQNT, priceNQT, orderFeeQNT);
        return createTransaction(req, account, attachment);
    }
}
