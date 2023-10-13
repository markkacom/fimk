package nxt.http.rpc;

import nxt.Account;
import nxt.Alias;
import nxt.Asset;
import nxt.Currency;
import nxt.DigitalGoodsStore;
import nxt.Account.AccountInfo;
import nxt.DigitalGoodsStore.Goods;
import nxt.MofoQueries;
import nxt.db.DbIterator;
import nxt.http.JSONResponses;
import nxt.http.ParameterException;
import nxt.http.websocket.JSONData;
import nxt.http.websocket.RPCCall;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class Search extends RPCCall {
    
    public static RPCCall instance = new Search("search");

    public Search(String identifier) {
        super(identifier);
    }
  
    @SuppressWarnings("unchecked")
    @Override
    public JSONStreamAware call(JSONObject arguments) throws ParameterException {
      
        String query = Convert.nullToEmpty((String) arguments.get("query"));
        String category = Convert.nullToEmpty((String) arguments.get("category"));
        int firstIndex = ParameterParser.getFirstIndex(arguments);
        int lastIndex = ParameterParser.getLastIndex(arguments);
  
        JSONObject response = new JSONObject();
        JSONArray results = new JSONArray();
        response.put("results", results);
        
        if (query == null) {
            return response;
        }
        
        if ("assets".equalsIgnoreCase(category)) {
            
            /* it might be an asset ID */
            try {
                long asset_id = Convert.parseUnsignedLong(query.substring(0, query.length()-1));
                if (asset_id != 0) {
                    Asset asset = Asset.getAsset(asset_id);
                    if (asset != null) {
                        JSONObject json = new JSONObject();
                        json.put("name", asset.getName());
                        json.put("decimals", asset.getDecimals());
                        json.put("description", asset.getDescription());
                        json.put("asset", Long.toUnsignedString(asset.getId()));
                        JSONData.putAccount(json, "account", asset.getAccountId());
                        results.add(json);
                    }
                }
            } catch (Exception e) {}
          
            try (DbIterator<Asset> assets = Asset.searchAssets(query, firstIndex, lastIndex)) {
                while (assets.hasNext()) {
                    Asset asset = assets.next();
                    JSONObject json = new JSONObject();
                    json.put("name", asset.getName());
                    json.put("decimals", asset.getDecimals());
                    json.put("description", asset.getDescription());
                    json.put("asset", Long.toUnsignedString(asset.getId()));
                    JSONData.putAccount(json, "account", asset.getAccountId());
                    results.add(json);
                }
            }
        }
        else if ("accounts".equalsIgnoreCase(category)) {
            
            /* it might be an RS or unsigned long account ID */
            try {
                long account_id = Convert.parseAccountId(query.substring(0, query.length()-1));
                if (account_id != 0) {
                    Account account = Account.getAccount(account_id);
                    if (account != null) {
                        JSONObject json = JSONData.accountBalance(account);
                        JSONData.putAccount(json, "account", account.getId());
                        results.add(json);                      
                    }
                }
            } catch (Exception e) {}
          
            try (DbIterator<AccountInfo> infos = Account.searchAccounts(query, firstIndex, lastIndex)) {
                while (infos.hasNext()) {
                    AccountInfo info = infos.next();
                    if (info != null) {
                        Account account = Account.getAccount(info.getAccountId());
                        if (account != null) {
                            JSONObject json = JSONData.accountBalance(account);
                            JSONData.putAccount(json, "account", account.getId());
                            results.add(json);
                        }
                    }
                }
            }
        }
        else if ("currencies".equalsIgnoreCase(category)) {
            try (DbIterator<Currency> currencies = Currency.searchCurrencies(query, firstIndex, lastIndex)) {
                while (currencies.hasNext()) {
                    Currency currency = currencies.next();

                    JSONObject json = new JSONObject();
                    json.put("name", currency.getName());
                    json.put("description", currency.getDescription());
                    json.put("currentSupply", currency.getCurrentSupply());
                    json.put("code", currency.getCode());
                    json.put("type", currency.getType());
                    json.put("decimals", currency.getDecimals());
                    json.put("issuanceHeight", currency.getIssuanceHeight());

                    results.add(json);
                }
            }
        }
        else if ("market".equalsIgnoreCase(category)) {
            try (DbIterator<Goods> goods = DigitalGoodsStore.Goods.searchGoods(query, false, firstIndex, lastIndex)) {
                while (goods.hasNext()) {
                    results.add(JSONData.goods(goods.next(), true));
                }
            }
        }
        else if ("aliases".equalsIgnoreCase(category)) {
            try (DbIterator<? extends Alias> aliases = MofoQueries.searchAlias(query, firstIndex, lastIndex)) {
                while (aliases.hasNext()) {
                    Alias alias = aliases.next();

                    JSONObject json = new JSONObject();
                    json.put("aliasName", alias.getAliasName());
                    json.put("aliasURI", alias.getAliasURI());
                    JSONData.putAccount(json, "account", alias.getAccountId());
                    
                    results.add(json);
                }
            }
        }
        else {
            return JSONResponses.ERROR_INCORRECT_REQUEST;
        }

        return response;
    }
}