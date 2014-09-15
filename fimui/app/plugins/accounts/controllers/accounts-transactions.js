(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('AccountsPluginTransactionsController', 
  function($scope, alerts, $timeout, ngTableParams, nxt, modals, plugins, db, $filter) {

  /* Show detail modal for account */
  $scope.showAccount = function (id_rs) {
    plugins.get('accounts').detail(id_rs);
  }; 

  /* ng-table config XXX - TODO look into https://datatables.net/ instead */ 
  $scope.transactions = [];
  $scope.tableParams = new ngTableParams({
      page: 1,   // show first page
      count: 10  // count per page
    }, 
    {
      total: 0,   // length of data
      getData: function($defer, params) {
        if ($scope.selectedAccount) {
          var list = $scope.transactions.slice((params.page() - 1) * params.count(), params.page() * params.count());
        }
        else {
          var list = [];
        }

        var transactions = [];
        angular.forEach(list, function (transaction) {
          // We are the sender
          if (transaction.senderRS == $scope.selectedAccount.id_rs) {
            var account = transaction.recipientRS;
            var fee     = nxt.util.convertToNXT(transaction.feeNQT);
            var amount  = nxt.util.convertToNXT(transaction.amountNQT);
          }
          // We are the recipient
          else {
            var account = transaction.senderRS;
            var fee     = nxt.util.convertToNXT(transaction.feeNQT);
            var amount  = nxt.util.convertToNXT(transaction.amountNQT);
          }

          transactions.push({
            transaction: transaction.transaction,
            type: transactionTypeToString(transaction.type, transaction.subtype),
            account: account,
            date: NRS.formatTimestamp(transaction.timestamp), /* XXX - TODO - fix relying on NRS */
            fee: fee,
            amount: amount,
            attachment: transaction.attachment,
            timestamp: transaction.timestamp
          });
        });
        $defer.resolve(transactions);
      }
    }
  );

  function find(array, id, value) {
    for(var i=0,l=array.length; i<l; i++) { if (array[i][id] == value) { return i; } }
    return -1;
  }

  function sorter(a,b) {
    return b.timestamp - a.timestamp;
  }

  function filter(array) {
    if ($scope.selectedAccount) {
      var id_rs = $scope.selectedAccount.id_rs
      return array.filter(function (t) { return t.senderRS == id_rs || t.recipientRS == id_rs });
    }
    return [];
  }

  $scope.$watch('selectedAccount', function (selectedAccount) {    
    $scope.transactions = [];
    if (!selectedAccount) return;

    /* Load transactions from database */
    db.transactions.where('senderRS').equals($scope.selectedAccount.id_rs).
                    or('recipientRS').equals($scope.selectedAccount.id_rs).toArray().then(
      function (transactions) {
        $timeout(function () {
          transactions.sort(sorter);
          $scope.transactions = transactions;
          $scope.tableParams.total(transactions.length);
          $scope.tableParams.reload(); 
        });
      }
    ).catch(alerts.catch("Could not load transactions from database"));
  });

  /* Register transactions CRUD observer */
  db.transactions.addObserver($scope, {
    create: function (transactions) {
      $scope.transactions = $scope.transactions.concat(filter(transactions));
      $scope.transactions.sort(sorter);
    },
    update: function (transactions) {
      angular.forEach(filter(transactions), function (t) {
        var index = find($scope.transactions, 'transaction', t.transaction);
        if (index != -1) {
          angular.extend($scope.transactions[index], t);
        }
      });
    },
    remove: function (transactions) {
      angular.forEach(filter(transactions), function (t) {
        var index = find($scope.transactions, 'transaction', t.transaction);
        if (index != -1) {
          $scope.transactions.splice(index, 1);
        }
      });
    },
    finally: function () { /* called from $timeout */
      $scope.tableParams.total($scope.transactions.length);
      $scope.tableParams.reload(); 
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