(function () {
'use strict';
var module = angular.module('dgex.base');

module.controller('orderSellController', function (items, $modalInstance, $scope, $timeout) {
  $scope.items = items;

  /* Buy order grid */
  var pair = $scope.items.selectedPair;
  $scope.gridColumnDefs = [
    {field:'price', displayName: 'Bid (' + pair.base.symbol + ')', cellClass: 'rightAlignCell'}, 
    {field:'quantity', displayName:  pair.quote.symbol, cellClass: 'rightAlignCell'},
    {field:'cumulative', displayName: 'Sum (' + pair.base.symbol + ')', cellClass: 'rightAlignCell'}
  ];    

  var temp = $scope.items.buyOrders;
  $scope.items.buyOrders = [];
  $scope.gridOptions = { 
    data: 'items.buyOrders',
    columnDefs: 'gridColumnDefs'
  };   
  $timeout(function () { $scope.items.buyOrders = temp; temp = null; }, 1500);


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