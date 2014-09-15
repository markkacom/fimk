(function () {
'use strict';
var module = angular.module('fim.base');

var _secretPhraseCache = {};

module.controller('secretPhraseModalController', function (items, $modalInstance, $scope, $timeout, nxt) {

  $scope.items = items;
  $scope.items.valid = false;
  $scope.items.remember = false;

  $scope.passphraseChange = function () {
    $timeout(function () {
      var accountID = nxt.crypto.getAccountId($scope.items.secretPhrase, true);
      var valid     = accountID == $scope.items.sender;
      $scope.items.valid = valid;
    });
  }  

  if (items.sender && items.sender in _secretPhraseCache) {
    $scope.items.remember = true;
    $scope.items.secretPhrase = _secretPhraseCache[items.sender];
    $scope.passphraseChange();
  }

  $scope.close = function () {
    if ($scope.items.remember) {
      _secretPhraseCache[items.sender] = $scope.items.secretPhrase;
    }    
    $modalInstance.close(angular.copy($scope.items));
  }

  $scope.dismiss = function () {
    $modalInstance.dismiss();
  }
});

})();