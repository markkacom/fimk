load("src/test/resources/lib/TestUtils.js");
load("src/test/resources/lib/Nxt.js");

function orderPriceNQT(priceNXT, decimals) {
  return Nxt.util.calculatePricePerWholeQNT(Nxt.util.convertToNQT(priceNXT), decimals)
}

function orderQuantityQNT(quantity, decimals) {
  return Nxt.util.convertToQNT(quantity, decimals);
}

tests({
  Before: function () {
    this.issuer = Nxt.createFundedAccount('123', '20000');
    this.buyer  = Nxt.createFundedAccount('buyer', '10000');
    this.seller = Nxt.createFundedAccount('seller', '10000');
    this.asset  = this.issuer.issueAsset('asset1', true, '1000000', 6);
    this.asset.setAccountAllowed(this.buyer.id_rs, true);
    this.asset.setAccountAllowed(this.seller.id_rs, true);
  },
  "Test events are dispatched": function () {
    Nxt.disableAutoForge(this, function () {
      Nxt.collectWebsocketEvents(this, function () {
        this.buyer.placeBidOrder(this.asset.asset, '10', '1', '0');
        jsAssert.assertEquals(1, Nxt.getWebsocketEvents('BID_ORDER_ADD').length);
        Nxt.clearWebsocketEvents();
        jsAssert.assertEquals(0, Nxt.getWebsocketEvents('BID_ORDER_ADD').length);
        jsAssert.assertEquals(0, Nxt.getWebsocketEvents('*').length);
      });
    });
  },
  "Test order and trade events": function () {
    Nxt.disableAutoForge(this, function () {
      Nxt.collectWebsocketEvents(this, function () {        
        var bid = this.buyer.placeBidOrder(this.asset.asset, '10', '1', '0');
        var ask = this.issuer.placeAskOrder(this.asset.asset, '10', '1', '0');
        var events = Nxt.getWebsocketEvents('TRADE,BID,ASK', true);

        assert.assertEquals('bid', events[0].type);
        assert.assertEquals(this.asset.quantityQNT('10'), events[0].quantityQNT);
        assert.assertEquals('ask', events[1].type);
        assert.assertEquals(this.asset.quantityQNT('10'), events[1].quantityQNT);
        assert.assertEquals('sell', events[2].tradeType);
        assert.assertEquals('ASK_ORDER_REMOVE*'+this.asset.asset, events[3].topic);
        assert.assertEquals('BID_ORDER_REMOVE*'+this.asset.asset, events[4].topic);
        
      });
    });
  },
  "Test tradeType sell": function () {
    Nxt.disableAutoForge(this, function () {
      Nxt.collectWebsocketEvents(this, function () {        
        var bid = this.buyer.placeBidOrder(this.asset.asset, '10', '1', '0');
        var ask = this.issuer.placeAskOrder(this.asset.asset, '10', '1', '0');
        assert.assertEquals('sell', Nxt.getWebsocketEvents('TRADE').first.tradeType);
      });
    });
  },
  "Test tradeType buy": function () {
    Nxt.disableAutoForge(this, function () {
      Nxt.collectWebsocketEvents(this, function () {        
        var ask = this.issuer.placeAskOrder(this.asset.asset, '10', '1', '0');
        var bid = this.buyer.placeBidOrder(this.asset.asset, '10', '1', '0');
        assert.assertEquals('buy', Nxt.getWebsocketEvents('TRADE').first.tradeType);
      });
    });
  },
  "Test bid is updated": function () {
    Nxt.disableAutoForge(this, function () {
      Nxt.collectWebsocketEvents(this, function () {
       this.issuer.placeAskOrder(this.asset.asset, '10', '3', '0');
       this.issuer.placeAskOrder(this.asset.asset, '10', '2', '0');
       this.issuer.placeAskOrder(this.asset.asset, '10', '1', '0');
       
       this.buyer.placeBidOrder(this.asset.asset, '25', '3', '0');

       console.log(Nxt.getWebsocketEvents('TRADE,ASK,BID', true));          
      });
    });
  },
  
  "Test unconfirmed bid orders are returned": function () {
    Nxt.disableAutoForge(this, function () {
      this.buyer.placeBidOrder(this.asset.asset, '10', '1', '0');
      this.buyer.placeBidOrder(this.asset.asset, '10', '2', '0');
      this.buyer.placeBidOrder(this.asset.asset, '10', '3', '0');
      this.buyer.placeBidOrder(this.asset.asset, '10', '4', '0');
      this.buyer.placeBidOrder(this.asset.asset, '10', '5', '0');      
      Nxt.generateBlock();
      
      /* these are the unconfirmed orders */
      this.buyer.placeBidOrder(this.asset.asset, '10', '1.1', '0');
      this.buyer.placeBidOrder(this.asset.asset, '10', '2.1', '0');
      this.buyer.placeBidOrder(this.asset.asset, '10', '3.1', '0');
      this.buyer.placeBidOrder(this.asset.asset, '10', '4.1', '0');
      this.buyer.placeBidOrder(this.asset.asset, '10', '5.1', '0');
      
      /* virtual orders should include both confirmed and unconfirmed */
      var sliced = this.asset.getVirtualBidOrders(0, 5);
      jsAssert.assertEquals(5, sliced.length);
      
      assert.assertEquals(sliced[0].priceNQT, orderPriceNQT('5.1', this.asset.decimals));
      assert.assertEquals(sliced[1].priceNQT, orderPriceNQT('5', this.asset.decimals));
      assert.assertEquals(sliced[2].priceNQT, orderPriceNQT('4.1', this.asset.decimals));
      assert.assertEquals(sliced[3].priceNQT, orderPriceNQT('4', this.asset.decimals));
      assert.assertEquals(sliced[4].priceNQT, orderPriceNQT('3.1', this.asset.decimals));

      Nxt.generateBlock();
      var sliced = this.asset.getVirtualBidOrders(5, 10);
      jsAssert.assertEquals(5, sliced.length);

      assert.assertEquals(sliced[0].priceNQT, orderPriceNQT('3', this.asset.decimals));
      assert.assertEquals(sliced[1].priceNQT, orderPriceNQT('2.1', this.asset.decimals));
      assert.assertEquals(sliced[2].priceNQT, orderPriceNQT('2', this.asset.decimals));
      assert.assertEquals(sliced[3].priceNQT, orderPriceNQT('1.1', this.asset.decimals));
      assert.assertEquals(sliced[4].priceNQT, orderPriceNQT('1', this.asset.decimals));
    });
  },
  "Test unconfirmed ask orders are returned": function () {
    this.issuer.transferAsset(this.seller.id_rs, this.asset.asset, '1000');    
    
    Nxt.disableAutoForge(this, function () {
      this.seller.placeAskOrder(this.asset.asset, '10', '1', '0');
      this.seller.placeAskOrder(this.asset.asset, '10', '2', '0');
      this.seller.placeAskOrder(this.asset.asset, '10', '3', '0');
      this.seller.placeAskOrder(this.asset.asset, '10', '4', '0');
      this.seller.placeAskOrder(this.asset.asset, '10', '5', '0');      
      Nxt.generateBlock();
      
      /* these are the unconfirmed orders */
      this.seller.placeAskOrder(this.asset.asset, '10', '1.1', '0');
      this.seller.placeAskOrder(this.asset.asset, '10', '2.1', '0');
      this.seller.placeAskOrder(this.asset.asset, '10', '3.1', '0');
      this.seller.placeAskOrder(this.asset.asset, '10', '4.1', '0');
      this.seller.placeAskOrder(this.asset.asset, '10', '5.1', '0');
      
      /* virtual orders should include both confirmed and unconfirmed */
      var sliced = this.asset.getVirtualAskOrders(0, 5);
      jsAssert.assertEquals(5, sliced.length);
      
      assert.assertEquals(sliced[0].priceNQT, orderPriceNQT('1', this.asset.decimals));
      assert.assertEquals(sliced[1].priceNQT, orderPriceNQT('1.1', this.asset.decimals));
      assert.assertEquals(sliced[2].priceNQT, orderPriceNQT('2', this.asset.decimals));
      assert.assertEquals(sliced[3].priceNQT, orderPriceNQT('2.1', this.asset.decimals));
      assert.assertEquals(sliced[4].priceNQT, orderPriceNQT('3', this.asset.decimals));

      Nxt.generateBlock();
      var sliced = this.asset.getVirtualAskOrders(5, 10);
      jsAssert.assertEquals(5, sliced.length);

      assert.assertEquals(sliced[0].priceNQT, orderPriceNQT('3.1', this.asset.decimals));
      assert.assertEquals(sliced[1].priceNQT, orderPriceNQT('4', this.asset.decimals));
      assert.assertEquals(sliced[2].priceNQT, orderPriceNQT('4.1', this.asset.decimals));
      assert.assertEquals(sliced[3].priceNQT, orderPriceNQT('5', this.asset.decimals));
      assert.assertEquals(sliced[4].priceNQT, orderPriceNQT('5.1', this.asset.decimals));
    });
  },
  "Test ask order quantities are updated while unconfirmed": function () {
    this.issuer.transferAsset(this.seller.id_rs, this.asset.asset, '1000');
    
    Nxt.disableAutoForge(this, function () {
      this.seller.placeAskOrder(this.asset.asset, '10', '1', '0');
      assert.assertEquals(this.asset.getVirtualAskOrders()[0].quantityQNT, orderQuantityQNT('10', this.asset.decimals));
      
      this.buyer.placeBidOrder(this.asset.asset, '5', '1', '0');
      assert.assertEquals(this.asset.getVirtualAskOrders()[0].quantityQNT, orderQuantityQNT('5', this.asset.decimals));
      assert.assertEquals(this.asset.getVirtualBidOrders()[0].quantityQNT, '0');
      
      Nxt.generateBlock();
      assert.assertEquals(this.asset.getVirtualAskOrders()[0].quantityQNT, orderQuantityQNT('5', this.asset.decimals));
      jsAssert.assertEquals(this.asset.getVirtualBidOrders().length, 0);
    });
  },    
  "Test bid order quantities are updated while unconfirmed": function () {
    this.issuer.transferAsset(this.seller.id_rs, this.asset.asset, '1000');    
    
    Nxt.disableAutoForge(this, function () {
      this.buyer.placeBidOrder(this.asset.asset, '4', '1', '0');
      assert.assertEquals(this.asset.getVirtualBidOrders()[0].quantityQNT, orderQuantityQNT('4', this.asset.decimals));
      
      this.seller.placeAskOrder(this.asset.asset, '1', '1', '0');
      assert.assertEquals(this.asset.getVirtualBidOrders()[0].quantityQNT, orderQuantityQNT('3', this.asset.decimals));
      assert.assertEquals(this.asset.getVirtualAskOrders()[0].quantityQNT, '0');
      
      Nxt.generateBlock();
      assert.assertEquals(this.asset.getVirtualBidOrders()[0].quantityQNT, orderQuantityQNT('3', this.asset.decimals));
      jsAssert.assertEquals(this.asset.getVirtualAskOrders().length, 0);
    });
  },    
  "Test trades are matched while ask orders are unconfirmed": function () {
    this.issuer.transferAsset(this.seller.id_rs, this.asset.asset, '1000');
    Nxt.disableAutoForge(this, function () {
      this.seller.placeAskOrder(this.asset.asset, '10', '1', '0');
      jsAssert.assertEquals(0, this.asset.getVirtualTrades().length);
      
      this.buyer.placeBidOrder(this.asset.asset, '5', '1', '0');
      jsAssert.assertEquals(1, this.asset.getVirtualTrades().length);
      assert.assertEquals(this.asset.getVirtualTrades()[0].quantityQNT, orderQuantityQNT('5', this.asset.decimals));
      
      Nxt.generateBlock();
      jsAssert.assertEquals(1, this.asset.getVirtualTrades().length);      
      assert.assertEquals(this.asset.getVirtualTrades()[0].quantityQNT, orderQuantityQNT('5', this.asset.decimals));
    });
  },
  "Test trades are matched while bid orders are unconfirmed": function () {
    this.issuer.transferAsset(this.seller.id_rs, this.asset.asset, '1000');
    Nxt.disableAutoForge(this, function () {
      this.buyer.placeBidOrder(this.asset.asset, '5', '1', '0');
      jsAssert.assertEquals(0, this.asset.getVirtualTrades().length);
      
      this.seller.placeAskOrder(this.asset.asset, '10', '1', '0');
      jsAssert.assertEquals(1, this.asset.getVirtualTrades().length);      
      assert.assertEquals(this.asset.getVirtualTrades()[0].quantityQNT, orderQuantityQNT('5', this.asset.decimals));
      
      Nxt.generateBlock();
      jsAssert.assertEquals(1, this.asset.getVirtualTrades().length);      
      assert.assertEquals(this.asset.getVirtualTrades()[0].quantityQNT, orderQuantityQNT('5', this.asset.decimals));
    });
  },
  "Test trades are correctly merged": function () {
    this.issuer.transferAsset(this.seller.id_rs, this.asset.asset, '1000');
    Nxt.disableAutoForge(this, function () {
      this.buyer.placeBidOrder(this.asset.asset, '2', '1', '0');
      this.buyer.placeBidOrder(this.asset.asset, '3', '1', '0');
      this.buyer.placeBidOrder(this.asset.asset, '4', '1', '0');
      this.buyer.placeBidOrder(this.asset.asset, '5', '1', '0');
      
      jsAssert.assertEquals(0, this.asset.getVirtualTrades().length);
      jsAssert.assertEquals(4, this.asset.getVirtualBidOrders().length);
      jsAssert.assertEquals(0, this.asset.getVirtualAskOrders().length);
      
      this.seller.placeAskOrder(this.asset.asset, '2', '1', '0');
      this.seller.placeAskOrder(this.asset.asset, '3', '1', '0');
      this.seller.placeAskOrder(this.asset.asset, '4', '1', '0');
      
      jsAssert.assertEquals(3, this.asset.getVirtualAskOrders().length);      
      jsAssert.assertEquals(3, this.asset.getVirtualTrades().length);
      
      Nxt.generateBlock();
      jsAssert.assertEquals(3, this.asset.getVirtualTrades().length);
      this.seller.placeAskOrder(this.asset.asset, '5', '1', '0');
      jsAssert.assertEquals(4, this.asset.getVirtualTrades().length);
      Nxt.generateBlock();
      jsAssert.assertEquals(4, this.asset.getVirtualTrades().length);
    });
  }
});