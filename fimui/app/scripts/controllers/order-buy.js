(function () {
'use strict';
var module = angular.module('fim.base');

/* items: {
    price: 1,
    amount: 2,
    total: 3,
    fee: 4,
    sellOrders: $scope.sellOrders,
    selectedPair: $scope.selectedPair,
    balance: {
      quote: getBalance($scope.selectedPair.quote),
      base: getBalance($scope.selectedPair.base)
    }
  } */
module.controller('orderBuyController', function (items, $modalInstance, $scope, $timeout) {
  $scope.items = items;

  /* Sell order grid */
  var pair = $scope.items.selectedPair;
  $scope.gridColumnDefs = [
    {field:'price', displayName: 'Ask (' + pair.base.symbol + ')', cellClass: 'rightAlignCell'}, 
    {field:'quantity', displayName:  pair.quote.symbol, cellClass: 'rightAlignCell'},
    {field:'cumulative', displayName: 'Sum (' + pair.base.symbol + ')', cellClass: 'rightAlignCell'}
  ];  

  var temp = $scope.items.sellOrders;
  $scope.items.sellOrders = [];
  $scope.gridOptions = { 
    data: 'items.sellOrders',
    columnDefs: 'gridColumnDefs'
  };   
  $timeout(function () { $scope.items.sellOrders = temp; temp = null; }, 1500);

  $scope.priceChanged = function () {
    $scope.items.total = $scope.items.amount * $scope.items.price;
    $scope.items.fee = $scope.items.total * 0.015;
  }

  $scope.amountChanged = function () {
    $scope.items.total = $scope.items.amount * $scope.items.price;
    $scope.items.fee = $scope.items.total * 0.015;
  }

  $scope.totalChanged = function () {
    $scope.items.amount = $scope.items.total / $scope.items.price;
    $scope.items.fee = $scope.items.total * 0.015;
  }

  $scope.highestBidClick = function () {
    $('form[name=buyForm] input[name=price]').val($scope.items.sellOrders[0].price).change();
  }

  $scope.availableClick = function () {
    $('form[name=buyForm] input[name=amount]').val($scope.items.balance.base).change();
  }

  $scope.close = function () {
    $modalInstance.close({
      price: $scope.items.price,
      amount: $scope.items.amount,
      total: $scope.items.total,
      fee: $scope.items.fee
    });
  }

  $scope.dismiss = function () {
    $modalInstance.dismiss();
  }

  /* Initialize entry fields */
  $scope.priceChanged();
});

})();