package nxt.http.rpc;

import java.util.List;

import nxt.Account;
import nxt.Asset;
import nxt.AssetTransfer;
import nxt.MofoAsset;
import nxt.MofoAsset.AssetFee;
import nxt.Trade;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;
import nxt.virtualexchange.VirtualTrade;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAsset extends RPCCall {

    public static RPCCall instance = new GetAsset("getAsset");

    public GetAsset(String identifier) {
        super(identifier);
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {

        boolean includeDetails = "true".equals((String) arguments.get("includeDetails"));
        boolean includeVolumes = "true".equals((String) arguments.get("includeVolumes"));

        Asset asset = ParameterParser.getAsset(arguments);
        JSONObject response = new JSONObject();
        response.put("decimals", asset.getDecimals());
        response.put("name", asset.getName());

        if (includeDetails) {
            response.put("description", asset.getDescription());
            JSONData.putAccount(response, "issuer", asset.getAccountId());
            response.put("quantityQNT", asset.getQuantityQNT());
            response.put("numberOfTrades", Trade.getTradeCount(asset.getId()));
            response.put("numberOfTransfers", AssetTransfer.getTransferCount(asset.getId()));
            response.put("numberOfAccounts", Account.getAssetAccountCount(asset.getId()));
        }

        if (includeVolumes) {
            // quantityQNTTotal
            // quantityQNTToday
            // numberOfTradesToday
            //MofoQueries.getAssetMetrics(response, asset.getId());
            List<VirtualTrade> iterator = VirtualTrade.getTrades(asset.getId(), 0, 0, 0);
            if (!iterator.isEmpty()) {
                response.put("lastPriceNQT", String.valueOf(iterator.get(0).getPriceNQT()));
            }
        }

        response.put("type", asset.getType());
        if (MofoAsset.isPrivateAsset(asset)) {
            AssetFee fee = MofoAsset.getFee(asset.getId());
            response.put("orderFeePercentage", fee.getOrderFeePercentage());
            response.put("tradeFeePercentage", fee.getTradeFeePercentage());
        }

        return response;
    }

}
