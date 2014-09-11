(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('secretPhraseModalController', function (items, $modalInstance, $scope, $timeout, nxt) {

  $scope.items = items;
  $scope.items.valid = false;

  $scope.close = function () {
    $modalInstance.close(angular.copy($scope.items));
  }

  $scope.dismiss = function () {
    $modalInstance.dismiss();
  }

  $scope.passphraseChange = function () {
    $timeout(function () {
      var accountID = nxt.crypto.getAccountId($scope.items.secretPhrase, true);
      var valid     = accountID == $scope.items.sender;
      $scope.items.valid = valid;
    });
  }
});

})();