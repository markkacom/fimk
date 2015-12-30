/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

load("src/test/resources/lib/TestUtils.js");
load("src/test/resources/lib/Nxt.js");

tests({
  "Test jsAssert.assertObjectEquals" : function() {
    jsAssert.assertObjectEquals(
      Nxt.call('getNonExistingAPI'), 
      {"errorDescription":"Incorrect request","errorCode":1}
    );
  },
  "Test Public Key": function () {
    assert.assertEquals("97d171fec9870d5ba98986e14c099c02f8e850ab188502a17bc0def21a6e392e", accounts.forger.publicKey);
  },
  "Test send money": function () {
    var account = new Account('123');
    accounts.forger.sendMoney(account.id_rs, '100');
    assert.assertEquals('100', account.getBalanceNXT());
    account.sendMoney(accounts.forger.id_rs, '50');
    assert.assertEquals('49.9', account.getBalanceNXT());
  },
  "Test Namespaced Alias": function () {
    accounts.a.setNamespacedAlias("abc", "hello");    
    assert.assertNotEquals('hello', accounts.a.getNamespacedAlias("abc"));    
  },  
  "Test Account constructor": function () {
    var account = new Account('123');
    assert.assertEquals('FIM-AWXM-L3EK-DTT7-6HM7U', account.id_rs);
    assert.assertEquals(null, account.getBalanceNXT());    
    accounts.a.sendMoney(account.id_rs, '10');
    assert.assertEquals('10', account.getBalanceNXT());
  },
  "Test issue asset": function () {
    var asset = accounts.a.issueAsset('asset1', true, '1000000', 8);
    assert.assertEquals('asset1', asset.name);
    assert.assertEquals(true, asset.isPrivate());
  },
  "Test Private asset": function () {
    var asset = accounts.a.issueAsset('asset1', true, '1000000', 8);
    assert.assertEquals(false, asset.getAccountAllowed(accounts.b.id_rs));
    asset.setAccountAllowed(accounts.b.id_rs);
    assert.assertEquals(true, asset.getAccountAllowed(accounts.b.id_rs));
    asset.setAccountAllowed(accounts.b.id_rs, false);
    assert.assertEquals(false, asset.getAccountAllowed(accounts.b.id_rs));
    
    asset.setOrderFeePercentage('0.2');
    assert.assertEquals('0.2', asset.orderFeePercentageHuman);
  },
  "Test fee percentage": function () {
    function getPercentage(str) {
      return Nxt.util.convertToQNT(str, 6);
    }    
    assert.assertEquals(getPercentage("0.000001"), "1");
    assert.assertEquals(getPercentage("0.00001"), "10");
    assert.assertEquals(getPercentage("0.000312"), "312");
    assert.assertEquals(getPercentage("0.0001"), "100");
    assert.assertEquals(getPercentage("0.001"), "1000");
    assert.assertEquals(getPercentage("0.01"), "10000");
    assert.assertEquals(getPercentage("0.1"), "100000");
    assert.assertEquals(getPercentage("1"), "1000000");
    assert.assertEquals(getPercentage("10"), "10000000");
    assert.assertEquals(getPercentage("100"),  "100000000");
    assert.assertEquals(getPercentage("1000"), "1000000000");
    assert.assertEquals(getPercentage("2000"), "2000000000");     
  },
  "Test cannot place ask order": function() {
    var asset = accounts.a.issueAsset('asset1', true, '1000000', 8);
    assert.assertEquals(true, asset.isPrivate());
    assert.assertEquals(false, asset.getAccountAllowed(accounts.b.id_rs));
    assert.assertEquals("Account not allowed to place bid order", accounts.b.placeBidOrder(asset.asset, '10', '1', '0').errorDescription);
  }  
});
