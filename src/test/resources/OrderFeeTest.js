load("src/test/resources/lib/TestUtils.js");
load("src/test/resources/lib/Nxt.js");

tests({
  Before: function () {
    this.issuer  = Nxt.createFundedAccount('123', '20000');
    this.account = Nxt.createFundedAccount('account', '10000');
    this.asset   = this.issuer.issueAsset('asset1', true, '1000000', 8);
    this.asset.setAccountAllowed(this.account.id_rs, true);
  },
  "Test bid order fee is transfered": function () {
    this.asset.setOrderFeePercentage('10'); // 10%
    this.issuer.setBalanceNXT('20000');    
    
    assert.assertEquals('20000', this.issuer.getBalanceNXT());
    assert.assertEquals('10000', this.account.getBalanceNXT());
    assert.assertEquals('Incorrect "orderFeeNQT" should be 1000000000', 
                        this.account.placeBidOrder(this.asset.asset, '100', '1', '20').errorDescription);
    assert.assertEquals('20000', this.issuer.getBalanceNXT());
    assert.assertEquals('10000', this.account.getBalanceNXT());                        
    assert.assertEquals(undefined, 
                        this.account.placeBidOrder(this.asset.asset, '100', '1', '10').errorDescription);
    assert.assertEquals('20010', this.issuer.getBalanceNXT());
    assert.assertEquals('9989.9', this.account.getBalanceNXT());
  },
  "Test rollback bid order fee transfer": function () {
    this.asset.setOrderFeePercentage('0.1'); // 0.1%
    this.issuer.setBalanceNXT('20000');
    
    assert.assertEquals('20000', this.issuer.getBalanceNXT());
    assert.assertEquals('10000', this.account.getBalanceNXT());

    var rollback = Nxt.getBlockchain().getHeight();
    
    this.account.placeBidOrder(this.asset.asset, '100', '1', '0.1');
    assert.assertEquals('20000.1', this.issuer.getBalanceNXT());
    assert.assertEquals('9999.8', this.account.getBalanceNXT());

    Nxt.rollback(rollback);

    assert.assertEquals('20000', this.issuer.getBalanceNXT());
    assert.assertEquals('10000', this.account.getBalanceNXT());
  },
  "Test bid order fee (2)": function () {
    this.asset.setOrderFeePercentage('10');
    
    this.issuer.setBalanceNXT('100');
    this.account.setBalanceNXT('100');
    
    var rollback = Nxt.getBlockchain().getHeight();
    
    this.account.placeBidOrder(this.asset.asset, '10', '1', '1'); // total 30, fee 3
    this.account.placeBidOrder(this.asset.asset, '10', '1', '1'); // total 30, fee 3
    this.account.placeBidOrder(this.asset.asset, '10', '1', '1'); // total 30, fee 3
    
    assertBalance(this.asset.asset, [
      [this.issuer,  '103', '1000000'],
      [this.account, '96.7', '0']
    ]);    
    Nxt.rollback(rollback);
    assertBalance(this.asset.asset, [
      [this.issuer,  '100', '1000000'],
      [this.account, '100', '0']
    ]);
  }
});