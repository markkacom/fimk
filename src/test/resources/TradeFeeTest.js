load("src/test/resources/lib/TestUtils.js");
load("src/test/resources/lib/Nxt.js");

tests({
  Before: function () {
    this.issuer  = Nxt.createFundedAccount('issuer', '2000');
    this.asset   = this.issuer.issueAsset('asset1', true, '100', 8);
    this.asset.setTradeFeePercentage('10');
    this.asset.setOrderFeePercentage('0');
    
    this.buyer1  = Nxt.createFundedAccount('buyer1', '1');
    this.buyer2  = Nxt.createFundedAccount('buyer2', '1');
    this.seller1 = Nxt.createFundedAccount('seller1', '1');
    this.seller2 = Nxt.createFundedAccount('seller2', '1');
    
    this.asset.setAccountAllowed(this.buyer1.id_rs, true);
    this.asset.setAccountAllowed(this.buyer2.id_rs, true);
    this.asset.setAccountAllowed(this.seller1.id_rs, true);
    this.asset.setAccountAllowed(this.seller2.id_rs, true);

    this.issuer.transferAsset(this.seller1.id_rs, this.asset.asset, '10');
    this.issuer.transferAsset(this.seller2.id_rs, this.asset.asset, '10');
    
    this.issuer.setBalanceNXT('100');
    this.buyer1.setBalanceNXT('100');
    this.buyer2.setBalanceNXT('100');
    this.seller1.setBalanceNXT('100');
    this.seller2.setBalanceNXT('100');
    
    assertBalance(this.asset.asset, [
      [this.issuer,  '100', '80'],
      [this.buyer1,  '100', '0'],
      [this.buyer2,  '100', '0'],
      [this.seller1, '100', '10'],
      [this.seller2, '100', '10'],
    ]);
  }, 
  "Test ask order fee is transfered": function () {
    this.buyer1.placeBidOrder(this.asset.asset, '1', '1', '0');
    this.buyer2.placeBidOrder(this.asset.asset, '1', '1', '0');
    jsAssert.assertEquals(2, this.asset.getBidOrders().length);
    
    var state1 = { 
      height: Nxt.getBlockchain().getHeight(), 
      balance:[
        [this.issuer,  '100', '80'],
        [this.buyer1,  '99.9', '0'],
        [this.buyer2,  '99.9', '0'],
        [this.seller1, '100', '10'],
        [this.seller2, '100', '10'],
      ]
    };
    assertBalance(this.asset.asset, state1.balance, 'state 1');
   
    this.seller1.placeAskOrder(this.asset.asset, '2', '1', '0');
    jsAssert.assertEquals(0, this.asset.getBidOrders().length);
    jsAssert.assertEquals(0, this.asset.getAskOrders().length);
    
    var state2 = {
      height: Nxt.getBlockchain().getHeight(), 
      balance:[
        [this.issuer,  '100.2', '80.2'],
        [this.buyer1,  '98.9', '0.9'],
        [this.buyer2,  '98.9', '0.9'],
        [this.seller1, '101.7', '8'],
        [this.seller2, '100', '10'],
      ]
    };    
    assertBalance(this.asset.asset, state2.balance, 'state 2');

    this.buyer1.placeBidOrder(this.asset.asset, '1', '1', '0');
    this.buyer2.placeBidOrder(this.asset.asset, '1', '1', '0');
   
    this.asset.setTradeFeePercentage('0');
    
    this.seller1.placeAskOrder(this.asset.asset, '1', '1', '0');
    this.seller2.placeAskOrder(this.asset.asset, '1', '1', '0');
   
    var state3 = {
      height: Nxt.getBlockchain().getHeight(), 
      balance:[
        [this.issuer,  '100.1', '80.2'],
        [this.buyer1,  '97.8', '1.9'],
        [this.buyer2,  '97.8', '1.9'],
        [this.seller1, '102.6', '7'],
        [this.seller2, '100.9', '9'],
      ]
    };    
    assertBalance(this.asset.asset, state3.balance, 'state 3');
    jsAssert.assertEquals(0, this.asset.getBidOrders().length);
    jsAssert.assertEquals(0, this.asset.getAskOrders().length);    

    Nxt.rollback(state3.height);
    assertBalance(this.asset.asset, state3.balance);
    Nxt.rollback(state2.height);
    assertBalance(this.asset.asset, state2.balance);
    Nxt.rollback(state1.height);
    assertBalance(this.asset.asset, state1.balance);    
    
    this.asset.setTradeFeePercentage('1');

    var state4 = {
      height: Nxt.getBlockchain().getHeight(), 
      balance:[
        [this.issuer,  '99.9', '80'],
        [this.buyer1,  '99.9', '0'],
        [this.buyer2,  '99.9', '0'],
        [this.seller1, '100',  '10'],
        [this.seller2, '100',  '10'],
      ]
    };
    assertBalance(this.asset.asset, state4.balance, 'state 4');
    jsAssert.assertEquals(2, this.asset.getBidOrders().length);
    jsAssert.assertEquals(0, this.asset.getAskOrders().length);
    
    this.seller1.placeAskOrder(this.asset.asset, '1', '1', '0');
    this.seller2.placeAskOrder(this.asset.asset, '1', '1', '0');
    jsAssert.assertEquals(0, this.asset.getAskOrders().length);
    jsAssert.assertEquals(0, this.asset.getBidOrders().length);

   
    var state5 = {
      height: Nxt.getBlockchain().getHeight(), 
      balance:[
        [this.issuer,  '99.92', '80.02'],
        [this.buyer1,  '98.9', '0.99'],
        [this.buyer2,  '98.9', '0.99'],
        [this.seller1, '100.89', '9'],
        [this.seller2, '100.89', '9'],
      ]
    };    
    assertBalance(this.asset.asset, state5.balance, 'state 5');    

    Nxt.rollback(state5.height, state5.balance);
    assertBalance(this.asset.asset, state5.balance);    
    Nxt.rollback(state4.height, state4.balance);
    assertBalance(this.asset.asset, state4.balance);
    Nxt.rollback(state1.height);
    assertBalance(this.asset.asset, state1.balance);    
  },
  "Test partial order full filled": function () {
    this.buyer1.placeBidOrder(this.asset.asset, '2', '1', '0');
    this.seller1.placeAskOrder(this.asset.asset, '1', '1', '0');
    assertBalance(this.asset.asset, [
      [this.issuer,  '100.1', '80.1'],
      [this.buyer1,  '98.9', '0.9'],
      [this.seller1, '100.8', '9']     
    ]);
    this.seller1.placeAskOrder(this.asset.asset, '1', '1', '0');
    assertBalance(this.asset.asset, [
      [this.issuer,  '100.2', '80.2'],
      [this.buyer1,  '97.9', '1.8'],
      [this.seller1, '101.6', '8']     
    ]);    
  }  
});