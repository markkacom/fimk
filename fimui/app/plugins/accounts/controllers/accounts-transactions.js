(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('AccountsPluginTransactionsController', function($scope, alerts, $timeout, ngTableParams, nxt, modals) {

  /* Show detail modal for account */
  $scope.showAccount = function (id_rs) {
    nxt.getAccount({account: id_rs}).then(
      function (account) {
        modals.open('accountsDetail', {
          resolve: {
            items: function () {
              angular.extend(account, {
                guaranteedBalanceNXT: NRS.convertToNXT(account.guaranteedBalanceNQT),
                balanceNXT: NRS.convertToNXT(account.balanceNQT),
                effectiveBalanceNXT: account.effectiveBalanceNXT,
                unconfirmedBalanceNXT: NRS.convertToNXT(account.unconfirmedBalanceNQT),
                forgedBalanceNXT: NRS.convertToNXT(account.forgedBalanceNQT)
              });
              return account;
            }
          }
        });
      },
      alerts.catch("Could not obtain account")
    );
  }; 

  $scope.$watch('selectedAccount', function () {
    if ($scope.selectedAccount) {
      $scope.selectedAccount.transactions().then(
        function (transactions) {
          $timeout(
            function () {
              var txns = [];
              angular.forEach(transactions, function (transaction) {

                // We are the sender
                if (transaction.senderRS == $scope.selectedAccount.id_rs) {
                  var account = transaction.recipientRS;
                  var fee     = UTILS.formatPrice(NRS.convertToNXT(transaction.feeNQT), 8);
                  var amount  = UTILS.formatPrice('-' + NRS.convertToNXT(transaction.amountNQT), 8);
                }
                // We are the recipient
                else {
                  var account = transaction.recipientRS;
                  var fee     = UTILS.formatPrice(NRS.convertToNXT(transaction.feeNQT), 8);
                  var amount  = UTILS.formatPrice(NRS.convertToNXT(transaction.amountNQT), 8);
                }

                txns.push({
                  transaction: transaction.transaction,
                  type: transactionTypeToString(transaction.type, transaction.subtype),
                  account: account,
                  date: NRS.formatTimestamp(transaction.timestamp),
                  fee: fee,
                  amount: amount,
                  attachment: transaction.attachment,
                  timestamp: transaction.timestamp
                });
              });

              txns.sort(function (a,b) {
                return b.timestamp - a.timestamp;
              });

              $scope.tableParams = new ngTableParams({
                  page: 1,            // show first page
                  count: 10           // count per page
                }, 
                {
                  total: txns.length,   // length of data
                  getData: function($defer, params) {
                    $defer.resolve(txns.slice((params.page() - 1) * params.count(), params.page() * params.count()));
                  }
                }
              );
            }
          );
        }
      ).catch(alerts.catch("Could not find transactions"));
    }
  });

  var TYPE_PAYMENT = 0
  var TYPE_MESSAGING = 1
  var TYPE_COLORED_COINS = 2
  var TYPE_DIGITAL_GOODS = 3
  var TYPE_ACCOUNT_CONTROL = 4

  var SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0

  var SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0
  var SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1
  var SUBTYPE_MESSAGING_POLL_CREATION = 2
  var SUBTYPE_MESSAGING_VOTE_CASTING = 3
  var SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT = 4
  var SUBTYPE_MESSAGING_ACCOUNT_INFO = 5

  var SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0
  var SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1
  var SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2
  var SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3
  var SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4
  var SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5

  var SUBTYPE_DIGITAL_GOODS_LISTING = 0
  var SUBTYPE_DIGITAL_GOODS_DELISTING = 1
  var SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE = 2
  var SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE = 3
  var SUBTYPE_DIGITAL_GOODS_PURCHASE = 4
  var SUBTYPE_DIGITAL_GOODS_DELIVERY = 5
  var SUBTYPE_DIGITAL_GOODS_FEEDBACK = 6
  var SUBTYPE_DIGITAL_GOODS_REFUND = 7    

  function transactionTypeToString(type, subtype) {
    switch (type) {
      case TYPE_PAYMENT: {
        switch (subtype) {
          case SUBTYPE_PAYMENT_ORDINARY_PAYMENT: return "Payment";
        }      
      }
      case TYPE_MESSAGING: {
        switch (subtype) {
          case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE: return "AM";
          case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:  return "Alias";
          case SUBTYPE_MESSAGING_POLL_CREATION: return "Poll";
          case SUBTYPE_MESSAGING_VOTE_CASTING: return "Vote"
          case SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT: return "Hub";
          case SUBTYPE_MESSAGING_ACCOUNT_INFO: return "Account Info"        
        }
      }
      case TYPE_COLORED_COINS: {
        switch (subtype) {
          case SUBTYPE_COLORED_COINS_ASSET_ISSUANCE: return "Asset";
          case SUBTYPE_COLORED_COINS_ASSET_TRANSFER: return "Asset Transfer";
          case SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT: return "Ask";
          case SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT: return "Bid";
          case SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION: return "Ask Cancel";
          case SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION: return "Bid Cancel";
        }
      }
      case TYPE_DIGITAL_GOODS: {
        return "DG"
      }
      case TYPE_ACCOUNT_CONTROL: {
        return "AC"
      }
    }
    return "Unknown";
  }

});

})();