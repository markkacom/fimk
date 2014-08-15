(function () {
'use strict';
var module = angular.module('dgex.base');

module.controller('asksController', function ($scope) {

  $scope.gridColumnDefs = [
    {field:'price', displayName:'Ask', cellClass: 'rightAlignCell'}, 
    {field:'quantity', displayName:'Size', cellClass: 'rightAlignCell'},
    {field:'cumulative', displayName:'Sum', cellClass: 'rightAlignCell'}
    //{field:'total', displayName:'Total', cellClass: 'rightAlignCell'},      
  ];

  $scope.gridOptions = { 
    data: 'sellOrders',
    columnDefs: 'gridColumnDefs'
  };    

  $scope.$watch('selectedPair', function () {
    if ($scope.selectedPair) {
      var pair = $scope.selectedPair;
      $scope.gridColumnDefs = [
        {field:'price', displayName: 'Ask (' + pair.base.symbol + ')', cellClass: 'rightAlignCell'}, 
        {field:'quantity', displayName:  pair.quote.symbol, cellClass: 'rightAlignCell'},
        {field:'cumulative', displayName: 'Sum (' + pair.base.symbol + ')', cellClass: 'rightAlignCell'}
      ];    
    }
  });    
});

})();