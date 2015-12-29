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

    // other accounts can assign an id to an account when they can provide a 
    // signed message of the desired id, signed by the account the id is assigned to 

    // the account having it's id assigned signs the id with his secret key
    var signature = Nxt.util.sign('name@fimk.fi', this.account1.secretPhrase);

    // now the assigning account can assign only that id and only when providing the signature
    this.account1.setAccountIdentifier('name@fimk.fi', this.account1.id_rs, signature, this.account2.secretPhrase);

    // and the name will assigned
    jsAssert.assertEquals(['name@fimk.fi',"XHV3-9KPX-GR9Q-GJPYS"], this.account1.getAccountIdentifiers());
  },
  "Test if signature is same as lompsa client": function () {
    var identifier      = 'aa@fimk.fi';
    var secretPhrase    = 'ashamed those became either ourselves jealous stone dawn delicate cast ourselves surprise'
    var signature       = Nxt.util.sign(identifier, secretPhrase);
    var signatory       = 'FIM-BTEQ-8FK2-2HC4-7LZRC';
    var publicKey       = 'dee5bc14b12ff8547ac1bd619f286d24cab4a330671a8e59bebcc96f96e8d069';

    var signature_check = 'fc0363add05867022841911deb1906a0088b15e2540575532e2ce2b3712d080c5946f4b5bef036151c97e1caa662bbedfe9fa82d268280e5ea1a3f2fa8b80fb4';

    jsAssert.assertEquals(signature, signature_check);

    signatory = Nxt.util.convertRSAddress(signatory);

    var arg = {
      secretPhrase: this.account2.secretPhrase,
      feeNQT: Nxt.util.convertToNQT('0.1'),
      deadline: '1440',
      recipient: signatory,
      identifier: identifier,
      signatory: signatory,
      signature: signature,
      recipientPublicKey: publicKey
    };
    var ret = Nxt.call('setAccountIdentifier', arg);
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return null; }
    if (Nxt.enableAutoForge) { Nxt.generateBlock(); }

    this.account1.setAccountIdentifier(identifier, signatory, signature, this.account2.secretPhrase);

  },
  "Other account can not set ID when providing signature for different ID": function () {
    var signature = Nxt.util.sign('name1@fimk.fi', this.account1.secretPhrase);
    var ret = this.account1.setAccountIdentifier('name2@fimk.fi', this.account1.id_rs, signature, this.account2.secretPhrase);
    
  }
});


