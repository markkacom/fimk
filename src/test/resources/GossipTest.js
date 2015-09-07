load("src/test/resources/lib/TestUtils.js");
load("src/test/resources/lib/Nxt.js");

tests({
  Before: function () {
    Nxt.verbose = true;
    this.sender = Nxt.createFundedAccount('sender', '10000');
    this.recipient = Nxt.createFundedAccount('recipient', '10000');
  },
  "Test gossip events": function () {
    Nxt.disableAutoForge(this, function () {
      Nxt.collectWebsocketEvents(this, function () {
        this.sender.sendGossip(this.recipient, "gossip-0", "1");
      
        jsAssert.assertEquals(2, Nxt.getWebsocketEvents("ADDEDGOSSIP*"+this.sender.id).length);
        jsAssert.assertEquals(1, Nxt.getWebsocketEvents("ADDEDGOSSIP*"+this.sender.id+"-1").length);
        jsAssert.assertEquals(2, Nxt.getWebsocketEvents("ADDEDGOSSIP#"+this.recipient.id).length);
        jsAssert.assertEquals(1, Nxt.getWebsocketEvents("ADDEDGOSSIP#"+this.recipient.id+"-1").length);
        jsAssert.assertEquals(1, Nxt.getWebsocketEvents("ADDEDGOSSIP-1").length);
        jsAssert.assertEquals(6, Nxt.getWebsocketEvents("ADDEDGOSSIP").length);

        Nxt.clearWebsocketEvents();
      });
    });      
  }
});