(function () {
'use strict';
var module = angular.module('dgex.base');

module.controller('accountsAddModalController', function (items, $modalInstance, $scope) {

  $scope.items = items;

  $scope.close = function () {
    $modalInstance.close({
      name: $scope.items.name,
      account_id: $scope.items.account_id,
      account_id_rs: $scope.items.account_id_rs
    });
  }

  $scope.dismiss = function () {
    $modalInstance.dismiss();
  }
});

})();