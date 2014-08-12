(function () {
'use strict';
var module = angular.module('dgex.base');

module.controller('accountsTransactionsController', function($scope, accountsService, serverService) {

$scope.transactions = [];
$scope.selectedTransactions = [];

$scope.gridColumnDefs = [
  {field:'transaction', displayName:'Transaction'}, 
  {field:'type', displayName:'Type'},
  {field:'account', displayName:'Account'},
  {field:'amount', displayName:'Amount', cellClass: 'rightAlignCell'},
  {field:'fee', displayName:'Fee', cellClass: 'rightAlignCell'},
  {field:'date', displayName:'Date'}
];

$scope.gridOptions = { 
  data: 'transactions',
  columnDefs: 'gridColumnDefs',
  multiSelect: false,
  keepLastSelected: true,
  enableRowSelection: true,
  selectedItems: $scope.selectedTransactions
}; 

$scope.$on('ngGridEventData', function(){
  $scope.gridOptions.selectRow(0, true);
});  

$scope.$watch('selectedAccount.transaction_ids.length', function () {
  console.log('Woohoo were hit');
  console.log($scope.selectedAccount);
  var transactions = [];
  if ($scope.selectedAccount) {
    var index = $scope.selectedAccount.transaction_ids.length > 100 ? -100 : 0;
    angular.forEach($scope.selectedAccount.transaction_ids.slice(index), function (_id) {
      var transaction = accountsService.transactions[_id];

      /* We are the sender */
      if (transaction.senderRS == $scope.selectedAccount.account_id_rs) {
        var account = transaction.recipientRS;
        var fee = UTILS.formatPrice(NRS.convertToNXT(transaction.feeNQT), 8);
        var amount = UTILS.formatPrice('-' + NRS.convertToNXT(transaction.amountNQT), 8);
      }
      /* We are the recipient */
      else {
        var account = transaction.recipientRS;
        var fee = UTILS.formatPrice(NRS.convertToNXT(transaction.feeNQT), 8);
        var amount = UTILS.formatPrice(NRS.convertToNXT(transaction.amountNQT), 8);
      }

      transactions.push({
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

    transactions.sort(function (a,b) {
      return b.timestamp - a.timestamp;
    });
  }
  $scope.transactions = transactions;
  console.log(transactions);
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