load("src/test/resources/lib/TestUtils.js");
load("src/test/resources/lib/Nxt.js");

tests({
  Before: function () {
    this.account = Nxt.createFundedAccount('123', '10000');
    this.asset = this.account.issueAsset('asset1', true, '1000000', 8);
  },
  After: function () {
    // this runs after every test
  },
  "Test issue private asset": function () {
    assert.assertTrue(this.asset.isPrivate());
    assert.assertFalse(this.account.issueAsset('asset2', false, '1000000', 8).isPrivate());
  },
  "Test set order fee percentage": function () {
    this.asset.setOrderFeePercentage('0.000001');
    assert.assertEquals(this.asset.orderFeePercentageHuman, '0.000001');

    this.asset.setOrderFeePercentage('0.000002');
    assert.assertEquals(this.asset.orderFeePercentageHuman, '0.000002');
    
    this.asset.setOrderFeePercentage('0.00111');
    assert.assertEquals(this.asset.orderFeePercentageHuman, '0.00111');
    
    this.asset.setOrderFeePercentage('110.000001');
    assert.assertEquals(this.asset.orderFeePercentageHuman, '110.000001');
    
    this.asset.setOrderFeePercentage('0');
    assert.assertEquals(this.asset.orderFeePercentageHuman, '0');
  },
  "Test set order fee percentage fails": function () {
    assert.assertEquals('Incorrect "orderFeePercentage"', this.asset.setFeeInternal('111000000001', '0').errorDescription);
    assert.assertEquals(undefined, this.asset.setFeeInternal('2000000000', '0').errorDescription);    
    assert.assertEquals('Incorrect "orderFeePercentage"', this.asset.setFeeInternal('2000000001', '0').errorDescription);
  },
  "Test set trade fee percentage": function () {
    this.asset.setTradeFeePercentage('0.000001');
    assert.assertEquals(this.asset.tradeFeePercentageHuman, '0.000001');

    this.asset.setTradeFeePercentage('0.000002');
    assert.assertEquals(this.asset.tradeFeePercentageHuman, '0.000002');
    
    this.asset.setTradeFeePercentage('0.00111');
    assert.assertEquals(this.asset.tradeFeePercentageHuman, '0.00111');
    
    this.asset.setTradeFeePercentage('110.000001');
    assert.assertEquals(this.asset.tradeFeePercentageHuman, '110.000001');
    
    this.asset.setTradeFeePercentage('0');
    assert.assertEquals(this.asset.tradeFeePercentageHuman, '0');
  },
  "Test set trade fee percentage fails": function () {
    assert.assertEquals('Incorrect "tradeFeePercentage"', this.asset.setFeeInternal('0', '111000000001').errorDescription);
    assert.assertEquals(undefined, this.asset.setFeeInternal('0', '2000000000').errorDescription);    
    assert.assertEquals('Incorrect "tradeFeePercentage"', this.asset.setFeeInternal('0', '2000000001').errorDescription);
  },
  "Test transfer": function () {
    var recipient = new Account('recipient');
    assert.assertEquals('1000000', this.account.getAssetBalance(this.asset.asset));
    assert.assertEquals("Recipient not allowed to receive private asset", this.account.transferAsset(recipient.id_rs, this.asset.asset, "100").errorDescription);
    assert.assertEquals('1000000', this.account.getAssetBalance(this.asset.asset));
    
    this.asset.setAccountAllowed(recipient.id_rs);
    this.account.transferAsset(recipient.id_rs, this.asset.asset, "100")
    assert.assertEquals('999900', this.account.getAssetBalance(this.asset.asset));
    assert.assertEquals('100', recipient.getAssetBalance(this.asset.asset));
  },
  "Test placeAskOrder not allowed": function () {
    var asker = Nxt.createFundedAccount('asker', '100');
    assert.assertEquals('Not enough assets', asker.placeAskOrder(this.asset.asset, 10, '2', '0').errorDescription);    
    this.asset.setAccountAllowed(asker.id_rs, true);
    this.account.transferAsset(asker.id_rs, this.asset.asset, '5');
    this.asset.setAccountAllowed(asker.id_rs, false);    
    assert.assertEquals('Account not allowed to place ask order', asker.placeAskOrder(this.asset.asset, 2, '2', '0').errorDescription);
  },
  "Test placeBidOrder not allowed": function () {
    var bidder = Nxt.createFundedAccount('bidder', '100');
    assert.assertEquals('Account not allowed to place bid order', bidder.placeBidOrder(this.asset.asset, 10, '2', '0').errorDescription);
    
    this.asset.setAccountAllowed(bidder.id_rs, true);
    var order = bidder.placeBidOrder(this.asset.asset, 10, '2', '0').transaction;
    assert.assertTrue(typeof order == 'string');
    bidder.cancelBidOrder(order);
    
    this.asset.setAccountAllowed(bidder.id_rs, false);
    assert.assertEquals('Account not allowed to place bid order', bidder.placeBidOrder(this.asset.asset, 10, '2', '0').errorDescription);    
  },  
  "Test rollback account-allowed": function () {
    var rollback = Nxt.getBlockchain().getHeight();    
    
    var recipient = new Account('recipient');
    assert.assertFalse(this.asset.getAccountAllowed(recipient.id_rs));    
    this.asset.setAccountAllowed(recipient.id_rs);
    assert.assertTrue(this.asset.getAccountAllowed(recipient.id_rs));
        
    Nxt.rollback(rollback);
    this.asset.reload();
    
    assert.assertFalse(this.asset.getAccountAllowed(recipient.id_rs));    
    this.asset.setAccountAllowed(recipient.id_rs);
    assert.assertTrue(this.asset.getAccountAllowed(recipient.id_rs));
  },
  "Test rollback fee": function () {
    assert.assertEquals(this.asset.tradeFeePercentageHuman, '0');
    assert.assertEquals(this.asset.orderFeePercentageHuman, '0');

    var rollback1 = Nxt.getBlockchain().getHeight();
    this.asset.setTradeFeePercentage('0.000002');
    this.asset.setOrderFeePercentage('0.000003');    
    assert.assertEquals(this.asset.tradeFeePercentageHuman, '0.000002');
    assert.assertEquals(this.asset.orderFeePercentageHuman, '0.000003');
    
    var rollback2 = Nxt.getBlockchain().getHeight();    
    this.asset.setTradeFeePercentage('0.1');
    this.asset.setOrderFeePercentage('0.2');
    assert.assertEquals(this.asset.tradeFeePercentageHuman, '0.1');
    assert.assertEquals(this.asset.orderFeePercentageHuman, '0.2');

    Nxt.rollback(rollback2);
    this.asset.reload();    
    assert.assertEquals(this.asset.tradeFeePercentageHuman, '0.000002');
    assert.assertEquals(this.asset.orderFeePercentageHuman, '0.000003');
    
    Nxt.rollback(rollback1);
    this.asset.reload();    
    assert.assertEquals(this.asset.tradeFeePercentageHuman, '0');
    assert.assertEquals(this.asset.orderFeePercentageHuman, '0');    
  }
  
});

