package nxt.http;

import nxt.BlockchainTest;
import nxt.Constants;
import nxt.MofoAsset;
import nxt.Nxt;
import nxt.MofoAsset.AssetFee;
import nxt.http.MofoHelper.MofoBuilder;
import nxt.util.Convert;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestPrivateAsset extends BlockchainTest {
  
    @Ignore
    public void issueAsset() {
        long quantityQNT = 1000 * Constants.ONE_NXT;
        short decimals = 8;
  
        MofoHelper.setSecretPhrase(secretPhrase1); // id1
        
        int rollback = Nxt.getBlockchain().getHeight();
        
        long assetRegular1 = MofoHelper.createAsset(quantityQNT, decimals);
        long assetRegular2 = MofoHelper.createAsset(quantityQNT, decimals);
        long assetPrivate1 = MofoHelper.createPrivateAsset(quantityQNT, decimals);
        long assetPrivate2 = MofoHelper.createPrivateAsset(quantityQNT, decimals);
      
        Assert.assertFalse(MofoAsset.isPrivateAsset(assetRegular1));
        Assert.assertFalse(MofoAsset.isPrivateAsset(assetRegular2));
        Assert.assertTrue(MofoAsset.isPrivateAsset(assetPrivate1));
        Assert.assertTrue(MofoAsset.isPrivateAsset(assetPrivate2));
        
        BlockchainTest.rollback(rollback);
        
        Assert.assertFalse(MofoAsset.isPrivateAsset(assetRegular1));
        Assert.assertFalse(MofoAsset.isPrivateAsset(assetRegular2));
        Assert.assertFalse(MofoAsset.isPrivateAsset(assetPrivate1));
        Assert.assertFalse(MofoAsset.isPrivateAsset(assetPrivate2));        
    }
    
    @Ignore
    public void removeAccountGivesSignatureError() {    
        long quantityQNT = 1000 * Constants.ONE_NXT;
        short decimals = 8;
  
        MofoHelper.setSecretPhrase(secretPhrase1);
        
        long assetPrivate1 = MofoHelper.createPrivateAsset(quantityQNT, decimals);
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate1, id2));

        setAccountAllowed(assetPrivate1, id2, true);
        Assert.assertTrue(MofoAsset.getAccountAllowed(assetPrivate1, id2));        
        setAccountAllowed(assetPrivate1, id2, false);
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate1, id2));
        setAccountAllowed(assetPrivate1, id2, true);
        Assert.assertTrue(MofoAsset.getAccountAllowed(assetPrivate1, id2));
        setAccountAllowed(assetPrivate1, id2, false);
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate1, id2));        
    }

    @Ignore
    public void privateAssetSetFeeAndRollback() {
        long quantityQNT = 1000 * Constants.ONE_NXT;
        short decimals = 8;

        MofoHelper.setSecretPhrase(secretPhrase1); // id1
        
        long assetPrivate1 = MofoHelper.createPrivateAsset(quantityQNT, decimals);
        long assetPrivate2 = MofoHelper.createPrivateAsset(quantityQNT, decimals);
        long assetRegular1 = MofoHelper.createAsset(quantityQNT, decimals);
        long assetRegular2 = MofoHelper.createAsset(quantityQNT, decimals);
        
        Assert.assertFalse(MofoAsset.isPrivateAsset(assetRegular1));
        Assert.assertFalse(MofoAsset.isPrivateAsset(assetRegular2));
        Assert.assertTrue(MofoAsset.isPrivateAsset(assetPrivate1));
        Assert.assertTrue(MofoAsset.isPrivateAsset(assetPrivate2));
      
        Assert.assertEquals(0, MofoAsset.getFee(assetRegular1).getOrderFeePercentage());
        Assert.assertEquals(0, MofoAsset.getFee(assetRegular2).getOrderFeePercentage());
        Assert.assertEquals(0, MofoAsset.getFee(assetPrivate1).getOrderFeePercentage());
        Assert.assertEquals(0, MofoAsset.getFee(assetPrivate2).getOrderFeePercentage());
      
        int rollback_1 = Nxt.getBlockchain().getHeight();
        
        setAssetFee(assetPrivate1, 1, 2);
        setAssetFee(assetPrivate2, 3, 4);
        
        Assert.assertEquals(1, MofoAsset.getFee(assetPrivate1).getOrderFeePercentage());
        Assert.assertEquals(2, MofoAsset.getFee(assetPrivate1).getTradeFeePercentage());
        Assert.assertEquals(3, MofoAsset.getFee(assetPrivate2).getOrderFeePercentage());
        Assert.assertEquals(4, MofoAsset.getFee(assetPrivate2).getTradeFeePercentage());
        
        int rollback_2 = Nxt.getBlockchain().getHeight();
        
        setAssetFee(assetPrivate1, 2, 3);
        setAssetFee(assetPrivate2, 4, 5);        
        
        Assert.assertEquals(2, MofoAsset.getFee(assetPrivate1).getOrderFeePercentage());
        Assert.assertEquals(3, MofoAsset.getFee(assetPrivate1).getTradeFeePercentage());
        Assert.assertEquals(4, MofoAsset.getFee(assetPrivate2).getOrderFeePercentage());
        Assert.assertEquals(5, MofoAsset.getFee(assetPrivate2).getTradeFeePercentage());
        
        BlockchainTest.rollback(rollback_2);
        
        Assert.assertEquals(1, MofoAsset.getFee(assetPrivate1).getOrderFeePercentage());
        Assert.assertEquals(2, MofoAsset.getFee(assetPrivate1).getTradeFeePercentage());
        Assert.assertEquals(3, MofoAsset.getFee(assetPrivate2).getOrderFeePercentage());
        Assert.assertEquals(4, MofoAsset.getFee(assetPrivate2).getTradeFeePercentage());
        
        BlockchainTest.rollback(rollback_1);
        
        Assert.assertEquals(0, MofoAsset.getFee(assetRegular1).getOrderFeePercentage());
        Assert.assertEquals(0, MofoAsset.getFee(assetRegular2).getOrderFeePercentage());
        Assert.assertEquals(0, MofoAsset.getFee(assetPrivate1).getOrderFeePercentage());
        Assert.assertEquals(0, MofoAsset.getFee(assetPrivate2).getOrderFeePercentage());
    }
    
    @Ignore
    public void privateAssetAddRemoveAccountAndRollback() {
        long quantityQNT = 1000 * Constants.ONE_NXT;
        short decimals = 8;
  
        MofoHelper.setSecretPhrase(secretPhrase1);
        
        long assetPrivate1 = MofoHelper.createPrivateAsset(quantityQNT, decimals);
        long assetPrivate2 = MofoHelper.createPrivateAsset(quantityQNT, decimals);      
  
        Assert.assertTrue(MofoAsset.isPrivateAsset(assetPrivate1));
        Assert.assertTrue(MofoAsset.isPrivateAsset(assetPrivate2));
        
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate1, id2));
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate2, id2));
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate1, id3));
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate2, id3));
        
        int rollback_1 = Nxt.getBlockchain().getHeight();
        
        setAccountAllowed(assetPrivate1, id2, true);
        setAccountAllowed(assetPrivate2, id2, true);
        setAccountAllowed(assetPrivate1, id3, true);
        
        Assert.assertTrue(MofoAsset.getAccountAllowed(assetPrivate1, id2));
        Assert.assertTrue(MofoAsset.getAccountAllowed(assetPrivate2, id2));
        Assert.assertTrue(MofoAsset.getAccountAllowed(assetPrivate1, id3));
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate2, id3));
        
        int rollback_2 = Nxt.getBlockchain().getHeight();
        
        setAccountAllowed(assetPrivate2, id3, true);
        Assert.assertTrue(MofoAsset.getAccountAllowed(assetPrivate2, id3));
      
        BlockchainTest.rollback(rollback_2);
        
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate2, id3));
      
        Assert.assertTrue(MofoAsset.getAccountAllowed(assetPrivate1, id2));
        Assert.assertTrue(MofoAsset.getAccountAllowed(assetPrivate2, id2));
        Assert.assertTrue(MofoAsset.getAccountAllowed(assetPrivate1, id3));
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate2, id3));
        
        BlockchainTest.rollback(rollback_1);
        
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate1, id2));
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate2, id2));
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate1, id3));
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate2, id3));
        
        setAccountAllowed(assetPrivate2, id3, true);
        setAccountAllowed(assetPrivate2, id3, true);
        setAccountAllowed(assetPrivate2, id3, true);
        
        Assert.assertTrue(MofoAsset.getAccountAllowed(assetPrivate2, id3));
        
        System.out.println("Set account allowed false");
        
        setAccountAllowed(assetPrivate2, id3, false);
        
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetPrivate2, id3));
    }
    
    @Ignore
    public void transferToNonAllowedAccountShouldFail() {
        MofoHelper.setSecretPhrase(secretPhrase1);
        long assetId = MofoHelper.createPrivateAsset(1000 * Constants.ONE_NXT, (short) 8);
        
        Assert.assertTrue(MofoAsset.isPrivateAsset(assetId));
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetId, id2));        
        
        MofoHelper.transferAsset(secretPhrase1, assetId, id2, 1000, "Recipient not allowed to receive private asset");
        setAccountAllowed(assetId, id2, true);
        
        MofoHelper.transferAsset(secretPhrase1, assetId, id2, 1000);
        generateBlock();
        
        Assert.assertEquals(1000, MofoHelper.getAssetBalance(assetId, id2));
       
        setAccountAllowed(assetId, id2, false);
        
        MofoHelper.transferAsset(secretPhrase2, assetId, id1, 1000, "Sender not allowed to transfer private asset");
        MofoHelper.transferAsset(secretPhrase1, assetId, id2, 1000, "Recipient not allowed to receive private asset");
        
        setAccountAllowed(assetId, id2, true);
        
        MofoHelper.transferAsset(secretPhrase2, assetId, id1, 500, "Sender not allowed to transfer private asset");
        generateBlock();
        
        Assert.assertEquals(500, MofoHelper.getAssetBalance(assetId, id2));
    }
    
    static void testFee(long assetId, int percentage, long amount, long result) {
        setAssetFee(assetId, percentage, percentage);
        Assert.assertEquals(result, MofoAsset.calculateOrderFee(assetId, amount));
    }
    
    @Ignore
    public void calculateOrderFee() {
        MofoHelper.setSecretPhrase(secretPhrase1);
        long assetId = MofoHelper.createPrivateAsset(1000 * Constants.ONE_NXT, (short) 8);

        // 0.000001%
        testFee(assetId, 1, 100000000, 1);
        testFee(assetId, 1, 99999999, 1);
        testFee(assetId, 1, 1, 1);
        testFee(assetId, 1, 200000000, 2);
        
        // 0.1%
        testFee(assetId, 100000, 1000, 1);
        testFee(assetId, 100000, 2000, 2);
        testFee(assetId, 100000, 999, 1);
        testFee(assetId, 100000, 1999, 2);
        
        // 1%
        testFee(assetId, 1000000, 100, 1);
        testFee(assetId, 1000000, 1, 1);
        testFee(assetId, 1000000, 10, 1);
        testFee(assetId, 1000000, 99, 1);

        // 10%
        testFee(assetId, 10000000, 100, 10);
        testFee(assetId, 10000000, 1, 1);
        testFee(assetId, 10000000, 10, 1);
        testFee(assetId, 10000000, 99, 10);
    }
    
    @Ignore
    public void placeOrderShouldFail() {
        MofoHelper.setSecretPhrase(secretPhrase1);
        long assetId = MofoHelper.createPrivateAsset(1000 * Constants.ONE_NXT, (short) 8);
        
        Assert.assertTrue(MofoAsset.isPrivateAsset(assetId));
        Assert.assertFalse(MofoAsset.getAccountAllowed(assetId, id2));        
        
        MofoHelper.placeBidOrder(secretPhrase2, assetId, 1 * Constants.ONE_NXT, 200, 0, "Account not allowed to place bid order");
        setAccountAllowed(assetId, id2, true);
        
        MofoHelper.placeBidOrder(secretPhrase2, assetId, 1 * Constants.ONE_NXT, 200, 0);
        setAssetFee(assetId, 1, 1);
        
        MofoHelper.placeBidOrder(secretPhrase2, assetId, 1 * Constants.ONE_NXT, 200, 0, "Insufficient \"orderFeeNQT\": minimum of 200 required");
        MofoHelper.placeBidOrder(secretPhrase2, assetId, 1 * Constants.ONE_NXT, 200, 200);        
    }
    
    @Test
    public void feeIsPaidForOrder() {
        MofoHelper.setSecretPhrase(secretPhrase1);
        long assetId = MofoHelper.createPrivateAsset(1000 * Constants.ONE_NXT, (short) 8);
        setAccountAllowed(assetId, id2, true);
        MofoHelper.transferAsset(secretPhrase1, assetId, id2, 1 * Constants.ONE_NXT);
        generateBlock();
        
        Assert.assertEquals(1 * Constants.ONE_NXT, MofoHelper.getAssetBalance(assetId, id2));
        Assert.assertEquals(999 * Constants.ONE_NXT, MofoHelper.getAssetBalance(assetId, id1));

        long issuerBalanceNQT = MofoHelper.getBalanceNQT(id1);
        long bidderBalanceNQT = MofoHelper.getBalanceNQT(id2);
        
        // fee is 0
        MofoHelper.placeBidOrder(secretPhrase2, assetId, 1000, 2, 0);
        
        Assert.assertEquals(issuerBalanceNQT, MofoHelper.getBalanceNQT(id1));
        Assert.assertEquals(bidderBalanceNQT, MofoHelper.getBalanceNQT(id2));

        Assert.assertEquals(1 * Constants.ONE_NXT, MofoHelper.getAssetBalance(assetId, id2));
        Assert.assertEquals(999 * Constants.ONE_NXT, MofoHelper.getAssetBalance(assetId, id1));

        // set fee at 1%
        setAssetFee(assetId, 1000000, 1000000);
        
        int height = Nxt.getBlockchain().getHeight();
        
        // account2 will place a buy order for 10 assets at 1 FIMK each
        // 10 assets in QNT is 1,000,000,000 (8 decimals)
        // 1 FIMK in NQT is      100,000,000 (8 decimals)
        // 
        // Total cost will be 1,000,000,000 * 100,000,000 = 1,00,000,000,000,000,000
        // 
        //
        //
        

        MofoHelper.placeBidOrder(secretPhrase2, assetId, 1000, 1, 0, "Insufficient \"orderFeeNQT\": minimum of 10 required");
        MofoHelper.placeBidOrder(secretPhrase2, assetId, 1000, 1, 10);
        
        Assert.assertEquals(issuerBalanceNQT + 10, MofoHelper.getBalanceNQT(id1));
        Assert.assertEquals(bidderBalanceNQT - 10, MofoHelper.getBalanceNQT(id2));        
        
        rollback(height);

        Assert.assertEquals(issuerBalanceNQT, MofoHelper.getBalanceNQT(id1));
        Assert.assertEquals(bidderBalanceNQT, MofoHelper.getBalanceNQT(id2));  
        
    }
    
    @Test
    public void feeIsPaidForBidOrder() {
      
    }
    
    public static void setAssetFee(long assetId, int orderFeePercentage, int tradeFeePercentage) {
        Assert.assertNotNull(MofoHelper.secretPhrase);
        MofoBuilder builder = MofoHelper.call(MofoHelper.secretPhrase, "setPrivateAssetFee");
        builder.asset(Convert.toUnsignedLong(assetId));
        builder.param("orderFeePercentage", orderFeePercentage);
        builder.param("tradeFeePercentage", tradeFeePercentage);
        
        MofoHelper.invoke(builder.build());
        BlockchainTest.generateBlock();
        
        AssetFee fee = MofoAsset.getFee(assetId);        
        Assert.assertEquals(orderFeePercentage, fee.getOrderFeePercentage());
        Assert.assertEquals(tradeFeePercentage, fee.getTradeFeePercentage());
    }
    
    public static void setAccountAllowed(long assetId, long accountId, boolean allowed) {
        Assert.assertNotNull(MofoHelper.secretPhrase);
        MofoBuilder builder = MofoHelper.call(MofoHelper.secretPhrase, allowed ? "addPrivateAssetAccount" : "removePrivateAssetAccount");
        builder.asset(Convert.toUnsignedLong(assetId));
        builder.recipient(accountId);
      
        MofoHelper.invoke(builder.build());
        BlockchainTest.generateBlock();        
        
        if (allowed) {
            Assert.assertTrue(MofoAsset.getAccountAllowed(assetId, accountId));
        }
        else {
            Assert.assertFalse(MofoAsset.getAccountAllowed(assetId, accountId));
        }
    }    
}
