package nxt.http.rpc;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;
import static nxt.http.JSONResponses.INCORRECT_ASSET;
import static nxt.http.JSONResponses.INCORRECT_FILTER;
import static nxt.http.JSONResponses.INCORRECT_ORDER;
import static nxt.http.JSONResponses.MISSING_ACCOUNT;
import static nxt.http.JSONResponses.MISSING_ASSET;
import static nxt.http.JSONResponses.MISSING_ORDER;
import static nxt.http.JSONResponses.UNKNOWN_ACCOUNT;
import static nxt.http.JSONResponses.UNKNOWN_ASSET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import nxt.Account;
import nxt.Asset;
import nxt.MofoQueries.TransactionFilter;
import nxt.MofoQueries.InclusiveTransactionFilter;
import nxt.http.ParameterException;
import nxt.util.Convert;
import nxt.util.JSON;


public class ParameterParser {
  
    static int getInt(JSONObject object, String name, int min, int max, boolean isMandatory) throws ParameterException {
        Object val = object.get(name);      
        String paramValue = val != null ? val.toString() : null;
        if (paramValue == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        int value;
        try {
            value = Integer.parseInt(paramValue);
        } 
        catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
        if (value < min || value > max) {
            throw new ParameterException(incorrect(name));
        }
        return value;
    }
    
    public static long getUnsignedLong(JSONObject object, String name, boolean isMandatory) throws ParameterException {
        String value = Convert.emptyToNull((String) object.get(name));
        if (value == null) {
            if (isMandatory) {
                throw new ParameterException(missing(name));
            }
            return 0;
        }
        try {
            return Convert.parseUnsignedLong(value);
        } catch (RuntimeException e) {
            throw new ParameterException(incorrect(name));
        }
    }    
    
    static String[] getArrayOfString(JSONObject object, String name, boolean isMandatory) throws ParameterException {
        Object paramValue = object.get(name);
        if (!(paramValue instanceof JSONArray)) {
            if (isMandatory) {
                throw new ParameterException(incorrect(name));
            }
            return null;
        }
        String[] result = new String[((JSONArray) paramValue).size()];
        for (int i=0; i<((JSONArray) paramValue).size(); i++) {
            result[i] = String.valueOf(((JSONArray) paramValue).get(i));
        }
        return result;
    }
    
    static int getTimestamp(JSONObject object) throws ParameterException {
        return getInt(object, "timestamp", 0, Integer.MAX_VALUE, false);
    }    
  
    static List<Long> getAccountIds(JSONObject object) throws ParameterException {
        String[] accountValues = getArrayOfString(object, "accounts", true);
        if (accountValues == null || accountValues.length == 0) {
            throw new ParameterException(MISSING_ACCOUNT);
        }
        List<Long> result = new ArrayList<>();
        for (String accountValue : accountValues) {
            if (accountValue == null || accountValue.equals("")) {
                continue;
            }
            try {
                result.add(Convert.parseAccountId(accountValue));
            } 
            catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ACCOUNT);
            }
        }
        return result;
    }    
    
    static Account getAccount(JSONObject object) throws ParameterException {
        String accountValue = Convert.emptyToNull((String) object.get("account"));
        if (accountValue == null) {
            throw new ParameterException(MISSING_ACCOUNT);
        }
        try {
            Account account = Account.getAccount(Convert.parseAccountId(accountValue));
            if (account == null) {
                throw new ParameterException(UNKNOWN_ACCOUNT);
            }
            return account;
        } 
        catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ACCOUNT);
        }
    }
    
    static Asset getAsset(JSONObject object) throws ParameterException {
        String assetValue = Convert.emptyToNull((String) object.get("asset"));
        if (assetValue == null) {
            throw new ParameterException(MISSING_ASSET);
        }
        Asset asset;
        try {
            long assetId = Convert.parseUnsignedLong(assetValue);
            asset = Asset.getAsset(assetId);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ASSET);
        }
        if (asset == null) {
            throw new ParameterException(UNKNOWN_ASSET);
        }
        return asset;      
    }
    
    static List<TransactionFilter> getTransactionFilter(JSONObject object) throws ParameterException {
        List<TransactionFilter> filter = new ArrayList<TransactionFilter>();
      
        String param = Convert.emptyToNull((String) object.get("transactionFilter"));
        if (param != null) {
            String[] pairs = param.split(",");
            for (String p : pairs) {
                
                String[] pair = p.split(":");
                if (pair.length != 2) {
                    throw new ParameterException(INCORRECT_FILTER);
                }
                
                try {
                    filter.add(new TransactionFilter(
                        Integer.parseInt(pair[0]), 
                        Integer.parseInt(pair[1])
                    ));
                }
                catch (NumberFormatException e) {
                    throw new ParameterException(INCORRECT_FILTER);
                }
            }
        }
        
        param = Convert.emptyToNull((String) object.get("inclusiveTransactionFilter"));
        if (param != null) {
            String[] pairs = param.split(",");
            for (String p : pairs) {
                
                String[] pair = p.split(":");
                if (pair.length != 2) {
                    throw new ParameterException(INCORRECT_FILTER);
                }
                
                try {
                    filter.add(new InclusiveTransactionFilter(
                        Integer.parseInt(pair[0]), 
                        Integer.parseInt(pair[1])
                    ));
                }
                catch (NumberFormatException e) {
                    throw new ParameterException(INCORRECT_FILTER);
                }
            }
        }        
        
        return filter;
    }
    
    @SuppressWarnings("unchecked")
    static JSONStreamAware missing(String... paramNames) {
        JSONObject response = new JSONObject();
        response.put("errorCode", 3);
        if (paramNames.length == 1) {
            response.put("errorDescription", "\"" + paramNames[0] + "\"" + " not specified");
        } 
        else {
            response.put("errorDescription", "At least one of " + Arrays.toString(paramNames) + " must be specified");
        }
        return JSON.prepare(response);
    }
  
    static JSONStreamAware incorrect(String paramName) {
        return incorrect(paramName, null);
    }
  
    @SuppressWarnings("unchecked")
    private static JSONStreamAware incorrect(String paramName, String details) {
        JSONObject response = new JSONObject();
        response.put("errorCode", 4);
        response.put("errorDescription", "Incorrect \"" + paramName + (details != null ? "\" " + details : "\""));
        return JSON.prepare(response);
    }
  
    /*@SuppressWarnings("unchecked")
    private static JSONStreamAware unknown(String objectName) {
        JSONObject response = new JSONObject();
        response.put("errorCode", 5);
        response.put("errorDescription", "Unknown " + objectName);
        return JSON.prepare(response);
    }*/
    
    static int getFirstIndex(JSONObject object) {
        int firstIndex;
        try {
            firstIndex = Integer.parseInt(object.get("firstIndex").toString());
            if (firstIndex < 0) {
                return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return firstIndex;
    }
  
    static int getLastIndex(JSONObject object) {
        int lastIndex;
        try {
            lastIndex = Integer.parseInt(object.get("lastIndex").toString());
            if (lastIndex < 0) {
                return Integer.MAX_VALUE;
            }
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
        return lastIndex;
    }

    public static long getOrderId(JSONObject object) throws ParameterException {
        String orderValue = Convert.emptyToNull((String) object.get("order"));
        if (orderValue == null) {
            throw new ParameterException(MISSING_ORDER);
        }
        try {
            return Convert.parseUnsignedLong(orderValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_ORDER);
        }
    }
}
