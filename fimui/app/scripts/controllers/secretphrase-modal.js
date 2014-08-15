(function () {
'use strict';
var module = angular.module('dgex.base');

module.controller('secretPhraseModalController', function (items, $modalInstance, $scope) {

  $scope.items = items;

  $scope.close = function () {
    $modalInstance.close({
      secretPhrase: $scope.items.secretPhrase
    });
  }

  $scope.dismiss = function () {
    $modalInstance.dismiss();
  }
});

})();