function GossipBuilder() {
  this.recipientPublicKey=null;
  this.recipient=null;
  this.message=null;
  this.topic=null;
  this.timestamp=Nxt.util.convertToEpochTimestamp(Date.now());
  this.senderPublicKey=null;
}
GossipBuilder.prototype = {
  setRecipientPublicKey: function (recipientPublicKey) {
    this.recipientPublicKey=recipientPublicKey;
    this.recipient=Nxt.util.getAccountIdFromPublicKey(recipientPublicKey);
    return this;
  },
  setMessage: function (message) {
    // encrypt the message here
    this.message=converters.stringToHexString(message);
    return this;
  },
  setTopic: function (topic) {
    this.topic=""+topic;
    return this;
  },
  build: function (secretPhrase) {
    this.senderPublicKey=Nxt.util.secretPhraseToPublicKey(secretPhrase);
    var arg = {
      senderPublicKey: this.senderPublicKey,
      recipient: this.recipient,
      message: this.message,
      timestamp: this.timestamp
    };
    if (this.topic) {
      arg.topic = this.topic;
    }
    var seed = [];
    seed.push(arg.timestamp, arg.recipient, arg.message);
    if (arg.topic) {
      seed.push(arg.topic);
    }
    var signatureseed = seed.join('');
    arg.signature = Nxt.util.sign(signatureseed, secretPhrase);
    arg.id = Nxt.util.getAccountIdFromPublicKey(arg.signature);
    return arg;
  }
}