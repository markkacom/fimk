package nxt.http;

import org.json.simple.JSONObject;
import org.junit.Assert;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.MofoAsset;
import nxt.MofoAsset.AssetFee;
import nxt.http.APICall.Builder;
import nxt.util.Convert;


public class MofoHelper {
  
    static int counter = 0;
    static String secretPhrase = null;

    static class MofoBuilder extends APICall.Builder {
        
        public MofoBuilder(String method) {
            super(method);
            feeNQT(Constants.ONE_NXT / 10);
        }
        
        public MofoBuilder asset(String asset) {
            param("asset", asset);
            return this;
        }
  
        public MofoBuilder asset(long assetId) {
            return this.asset(Convert.toUnsignedLong(assetId));
        }
        
        public MofoBuilder recipient(long accountId) {
            param("recipient", Convert.toUnsignedLong(accountId));
            return this;
        }

        public MofoBuilder account(long accountId) {
            param("account", Convert.toUnsignedLong(accountId));
            return this;
        }
        
        public MofoBuilder quantityQNT(long quantityQNT) {
            param("quantityQNT", Convert.toUnsignedLong(quantityQNT));
            return this;
        }

        public MofoBuilder priceNQT(long priceNQT) {
            param("priceNQT", Convert.toUnsignedLong(priceNQT));
            return this;
        }

        public MofoBuilder orderFeeNQT(long orderFeeNQT) {
            param("orderFeeNQT", Convert.toUnsignedLong(orderFeeNQT));
            return this;
        }
        
        public MofoBuilder orderFeeQNT(long orderFeeQNT) {
            param("orderFeeQNT", Convert.toUnsignedLong(orderFeeQNT));
            return this;
        }
    }    
    
    public static MofoBuilder call(String method) {
        return call(null, method);
    }

    public static void setSecretPhrase(String secretPhrase) {
        MofoHelper.secretPhrase = secretPhrase;
    }
    
    public static MofoBuilder call(String secretPhrase, String method) {
        MofoBuilder builder = new MofoBuilder(method);
        if (secretPhrase != null) {
            return (MofoBuilder) builder.secretPhrase(secretPhrase);
        }
        return builder;
    }
    
    public static long createAsset(long quantityQNT, short decimals) {
      return createAsset(createName(),"", false, quantityQNT, decimals);
    }  
  
    public static long createPrivateAsset(long quantityQNT, short decimals) {
        return createAsset(createName(),"", true, quantityQNT, decimals);
    }
    
    public static void setAssetFee(long assetId, int orderFeePercentage, int tradeFeePercentage) {
        Assert.assertNotNull(secretPhrase);
        MofoBuilder builder = call(secretPhrase, "setPrivateAssetFee");
        builder.asset(Convert.toUnsignedLong(assetId));
        builder.param("orderFeePercentage", orderFeePercentage);
        builder.param("tradeFeePercentage", tradeFeePercentage);
        
        invoke(builder.build());
        BlockchainTest.generateBlock();
        
        AssetFee fee = MofoAsset.getFee(assetId);        
        Assert.assertEquals(orderFeePercentage, fee.getOrderFeePercentage());
        Assert.assertEquals(tradeFeePercentage, fee.getTradeFeePercentage());
    }
    
    public static void setAccountAllowed(long assetId, long accountId, boolean allowed) {
        Assert.assertNotNull(secretPhrase);
        MofoBuilder builder = call(secretPhrase, allowed ? "addPrivateAssetAccount" : "removePrivateAssetAccount");
        builder.asset(Convert.toUnsignedLong(assetId));
        builder.recipient(accountId);
      
        invoke(builder.build());
        BlockchainTest.generateBlock();        
        
        if (allowed) {
            Assert.assertTrue(MofoAsset.getAccountAllowed(assetId, accountId));
        }
        else {
            Assert.assertFalse(MofoAsset.getAccountAllowed(assetId, accountId));
        }
    }
    
    public static void transferAsset(String secretPhrase, long assetId, long recipientId, long quantityQNT) {
        Builder builder = call(secretPhrase, "transferAsset").asset(assetId).recipient(recipientId).param("quantityQNT", String.valueOf(quantityQNT));
        invoke(builder.build());
        BlockchainTest.generateBlock();
    }
    
    public static void transferAsset(String secretPhrase, long assetId, long recipientId, long quantityQNT, String expected_error) {
        Builder builder = call(secretPhrase, "transferAsset").asset(assetId).recipient(recipientId).param("quantityQNT", String.valueOf(quantityQNT));
        invoke(builder.build(), true, expected_error);
        BlockchainTest.generateBlock();
    }
    
    public static long getAssetBalance(long assetId, long accountId) {
        Builder builder = MofoHelper.call("getAccountAssets").asset(assetId).account(accountId);
        JSONObject response = invoke(builder.build());
        return Convert.parseUnsignedLong((String) response.get("quantityQNT"));
    }
    
    public static long getBalanceNQT(long accountId) {
        Builder builder = MofoHelper.call("getBalance").account(accountId);
        JSONObject response = invoke(builder.build());
        return Convert.parseUnsignedLong((String) response.get("balanceNQT"));
    }
    
    public static long getUnconfirmedBalanceNQT(long accountId) {
        Builder builder = MofoHelper.call("getBalance").account(accountId);
        JSONObject response = invoke(builder.build());
        return Convert.parseUnsignedLong((String) response.get("unconfirmedBalanceNQT"));
    }    
    
    public static void placeBidOrder(String secretPhrase, long assetId, long quantityQNT, long priceNQT, long orderFeeNQT, String expected_error) {
        Builder builder = call(secretPhrase, "placeBidOrder").asset(assetId).quantityQNT(quantityQNT).priceNQT(priceNQT).orderFeeNQT(orderFeeNQT);
        invoke(builder.build(), true, expected_error); 
        BlockchainTest.generateBlock();
    }
    
    public static void placeBidOrder(String secretPhrase, long assetId, long quantityQNT, long priceNQT, long orderFeeNQT) {
        Builder builder = call(secretPhrase, "placeBidOrder").asset(assetId).quantityQNT(quantityQNT).priceNQT(priceNQT).orderFeeNQT(orderFeeNQT);
        invoke(builder.build()); 
        BlockchainTest.generateBlock();
    }

    public static void placeAskOrder(String secretPhrase, long assetId, long quantityQNT, long priceNQT, long orderFeeQNT, String expected_error) {
        Builder builder = call(secretPhrase, "placeAskOrder").asset(assetId).quantityQNT(quantityQNT).priceNQT(priceNQT).orderFeeQNT(orderFeeQNT);
        invoke(builder.build(), true, expected_error); 
        BlockchainTest.generateBlock();
    }
    
    public static void placeAskOrder(String secretPhrase, long assetId, long quantityQNT, long priceNQT, long orderFeeQNT) {
        Builder builder = call(secretPhrase, "placeAskOrder").asset(assetId).quantityQNT(quantityQNT).priceNQT(priceNQT).orderFeeQNT(orderFeeQNT);
        invoke(builder.build()); 
        BlockchainTest.generateBlock();
    }
    
    static long createAsset(String name, String description, boolean is_private, long quantityQNT, short decimals) {
        Assert.assertNotNull(secretPhrase);
        MofoBuilder builder = call(secretPhrase, "issueAsset");
        builder.feeNQT(1000 * Constants.ONE_NXT);
        builder.param("name", name);
        builder.param("description", description);
        builder.param("quantityQNT", quantityQNT);
        builder.param("decimals", decimals);
        builder.param("type", is_private ? (byte) 1 : (byte) 0);
        
        JSONObject response = invoke(builder.build());        
        String assetId = (String) response.get("transaction");
        BlockchainTest.generateBlock();
        
        response = call("getAsset").asset(assetId).build().invoke();
        Assert.assertEquals(assetId, response.get("asset"));        
        return Convert.parseUnsignedLong(assetId);
    }
    
    static String createName() {
        return "Name"+(counter++);
    }
    
    static JSONObject invoke(APICall apiCall) {
        return invoke(apiCall, true, null);
    }
  
    static JSONObject invoke(APICall apiCall, boolean must_return_success, String expected_error) {
        JSONObject result = apiCall.invoke();
        if (result.containsKey("error") || result.containsKey("errorDescription")) {
            String error = (String) result.get(result.containsKey("error") ? "error" : "errorDescription");
            if (must_return_success) {
                if (expected_error != null) {
                    if (expected_error.equalsIgnoreCase(error)) {
                        System.out.println(result.toJSONString());
                        return result;
                    }
                }
                System.out.println("ERROR " + error);                
                Assert.assertTrue(false);
            }
        }
        System.out.println(result.toJSONString());
        return result;
    }    
}
