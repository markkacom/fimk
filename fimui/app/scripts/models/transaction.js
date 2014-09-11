(function () {
'use strict';
var module = angular.module('fim.base');
module.factory('Transaction', function() {
  var Transaction = null;
  return {
    initialize: function (db) {
      Transaction = db.transactions.defineClass({
        type: Number,
        subtype: Number,
        timestamp: Number,
        deadline: Number,
        senderPublicKey: String,
        recipientRS: String,
        recipient: String,
        amountNQT: String,
        feeNQT: String,
        referencedTransactionFullHash: String,
        signature: String,
        signatureHash: String,
        fullHash: String,
        transaction: String, // id primary key
        // attachment: {}, XXX - TODO handle attachments
        senderRS: String,
        sender: String,
        height: Number,
        version: Number,
        ecBlockId: String,
        ecBlockHeight: String,
        block: String, // block id
        confirmations: Number,
        blockTimestamp: Number
      });

      Transaction.prototype.save = function () {
        return db.transactions.put(this);
      };

      return Transaction;
    },
    get: function () {
      return Transaction;
    }
  };

  return Transaction;
});

})();