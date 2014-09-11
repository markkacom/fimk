(function () {
'use strict';
var module = angular.module('fim.base');
module.factory('Account', function() {
  var Account = null;
  return {
    initialize: function (db) {
      Account = db.accounts.defineClass({
        id: String,
        id_rs: String,
        name: String,
        passphrase: String,
        guaranteedBalanceNXT: String,
        balanceNXT: String,
        effectiveBalanceNXT: String,
        unconfirmedBalanceNXT: String,
        forgedBalanceNXT: String
      });

      Account.prototype.save = function () {
        return db.accounts.put(this);
      };

      Account.prototype.delete = function () {
        return db.accounts.delete(this.id_rs);
      };

      Account.prototype.update = function (properties) {
        angular.extend(this, properties);
        return db.accounts.update(this.id_rs, properties);
      };

      Account.prototype.transactions = function () {
        return db.transactions.where('recipientRS').equals(this.id_rs).or('senderRS').equals(this.id_rs).toArray();
      };

      return Account;
    },
    get: function () {
      return Account;
    }
  };
});

})();