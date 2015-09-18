load("src/test/resources/lib/TestUtils.js");
load("src/test/resources/lib/Nxt.js");

tests({
  Before: function () {
    this.master = new Account(forgerSecretPhrase);
    this.verified1 = Nxt.createFundedAccount('verified1', '10000');
    this.verified2 = Nxt.createFundedAccount('verified2', '10000');
    this.account1 = Nxt.createFundedAccount('account1', '10000');
    this.account2 = Nxt.createFundedAccount('account2', '10000');
  },
  "Test account can set its own ID": function () {
    return;
    this.account1.setAccountIdentifier('aa@fimk.fi');
    jsAssert.assertEquals(['aa@fimk.fi'], this.account1.getAccountIdentifiers());

    this.account1.setAccountIdentifier('bb@fimk.fi');
    jsAssert.assertEquals(['bb@fimk.fi','aa@fimk.fi'], this.account1.getAccountIdentifiers());    
  },
  "Other account can set ID when providing signature": function () {
    return;
    // other accounts can assign an id to an account when they can provide a 
    // signed message of the desired id, signed by the account the id is assigned to 
    
    // the account having it's id assigned signs the id with his secret key
    var signature = Nxt.util.sign('name@fimk.fi', this.account1.secretPhrase);
    
    // now the assigning account can assign only that id and only when providing the signature
    this.account1.setAccountIdentifier('name@fimk.fi', this.account1.id_rs, signature, this.account2.secretPhrase);
    
    // and the name will assigned
    jsAssert.assertEquals(['name@fimk.fi'], this.account1.getAccountIdentifiers());
  },
  "Other account can not set ID when providing signature for different ID": function () {
    var signature = Nxt.util.sign('name1@fimk.fi', this.account1.secretPhrase);
    var ret = this.account1.setAccountIdentifier('name2@fimk.fi', this.account1.id_rs, signature, this.account2.secretPhrase);
    
  }
});