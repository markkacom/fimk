(function () {
'use strict';
var module = angular.module('dgex.base');

module.controller('bidsController', function ($scope) {

  $scope.gridColumnDefs = [
    {field:'cumulative', displayName:'Sum', cellClass: 'rightAlignCell'},
    {field:'quantity', displayName:'Size', cellClass: 'rightAlignCell'},
    {field:'price', displayName:'Bid', cellClass: 'rightAlignCell'}
  ];

  $scope.gridOptions = { 
    data: 'buyOrders',
    columnDefs: 'gridColumnDefs'
  };   

  $scope.$watch('selectedPair', function () {
    if ($scope.selectedPair) {
      var pair = $scope.selectedPair;
      $scope.gridColumnDefs = [
        {field:'cumulative', displayName: 'Sum (' + pair.base.symbol + ')', cellClass: 'rightAlignCell'},
        {field:'quantity', displayName: pair.quote.symbol, cellClass: 'rightAlignCell'},
        {field:'price', displayName: 'Bid (' + pair.base.symbol + ')', cellClass: 'rightAlignCell'}
      ];    
    }
  });
});
})();