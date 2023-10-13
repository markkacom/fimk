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
  Before: function () {
    this.issuer  = Nxt.createFundedAccount('issuer', '2000');
    this.asset   = this.issuer.issueAsset('asset1', true, '100', 8);
    this.buyer1  = Nxt.createFundedAccount('buyer1', '100');
    this.asset.setAccountAllowed(this.buyer1.id_rs, true);
  }, 
  "Ask order are cancelled when allowed status revoked": function () {
    this.buyer1.placeBidOrder(this.asset.asset, '1', '1', '0');
    this.buyer1.placeBidOrder(this.asset.asset, '2', '3', '0');
    this.buyer1.placeBidOrder(this.asset.asset, '3', '5', '0');
    jsAssert.assertEquals(3, this.asset.getBidOrders().length);
    
    this.asset.setAccountAllowed(this.buyer1.id_rs, false);
    jsAssert.assertEquals(0, this.asset.getBidOrders().length);
  }  
});