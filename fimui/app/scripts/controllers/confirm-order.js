(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('confirmOrderController', function ($scope, $rootScope) {
  $scope.confirm = function () {
    var order = $rootScope.order;
    var method = order.type == 'sell' ? 'placeAsk' : 'placeBid';
    $rootScope.exchange[method](order.selectedPair.quote, order.price, order.amount).then(
      function (data) {
        $rootScope.confirmModal.close();
      },
      function (error) {

      }
    );
  }
});

})();