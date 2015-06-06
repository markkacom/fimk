package nxt.http.rpc;

import java.util.Iterator;

import nxt.Asset;
import nxt.MofoAsset;
import nxt.MofoQueries;
import nxt.Nxt;
import nxt.MofoQueries.PrivateAccount;
import nxt.http.ParameterException;
import nxt.http.websocket.RPCCall;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAssetPrivateAccounts extends RPCCall {
    
    public static RPCCall instance = new GetAssetPrivateAccounts("getAssetPrivateAccounts");

    public GetAssetPrivateAccounts(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
        Asset asset = ParameterParser.getAsset(arguments);
        int firstIndex = ParameterParser.getFirstIndex(arguments);
        int lastIndex = ParameterParser.getLastIndex(arguments);
        boolean includeAllowed = true;
        boolean includeAll = arguments.containsKey("allowed") == false;
        if (!includeAll) {
            includeAllowed = Boolean.TRUE.equals(arguments.get("allowed"));
        }
        
        JSONObject response = new JSONObject();
        JSONArray accounts = new JSONArray();
        response.put("accounts", accounts);
        int index = 0;
        Iterator<PrivateAccount> iterator = MofoQueries.getAssetPrivateAccounts(asset.getId(), includeAllowed, includeAll, firstIndex, lastIndex).iterator();
        while (iterator != null && iterator.hasNext()) {
            PrivateAccount a = iterator.next();
            JSONObject obj = new JSONObject();
            obj.put("index", (index++)+firstIndex);
            obj.put("confirmations", Nxt.getBlockchain().getHeight() - a.height);
            obj.put("id_rs", Convert.rsAccount(a.account.getId()));
            obj.put("name", a.account.getName());
            obj.put("description", a.account.getDescription());
            obj.put("quantityQNT", a.account.getAssetBalanceQNT(asset.getId()));
            obj.put("asset", Convert.toUnsignedLong(asset.getId()));
            obj.put("decimals", asset.getDecimals());
            obj.put("allowed", a.allowed);
            accounts.add(obj);
        }
        return response;
    }  
}
