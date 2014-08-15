(function () {
'use strict';
var module = angular.module('dgex.base');

module.controller('tradesController', function ($scope) {
  $scope.gridColumnDefs = [
    {field:'time', displayName: 'Time'}, 
    {field:'price', displayName: 'Price', cellClass: 'rightAlignCell'},
    {field:'quantity', displayName: 'Volume', cellClass: 'rightAlignCell'},
    //{field:'total', displayName: 'Total', cellClass: 'rightAlignCell'},
    //{field:'askOrder', displayName: 'Sell Order'}, 
    //{field:'bidOrder', displayName: 'Buy Order'}, 
  ];

  $scope.gridOptions = { 
    data: 'trades',
    columnDefs: 'gridColumnDefs'
  };

  $scope.getBodyStyle = function () {
    return {
      'height': '100px',
      'overflow-y': 'scroll'
    }
  }
});

})();