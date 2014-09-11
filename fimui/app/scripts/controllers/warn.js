(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('warnController', function (items, $modalInstance, $scope) {

  /* Items: title: '' message: '' */
  $scope.items = items;
  $scope.close = function () {
    $modalInstance.close();
  }
  
});

})();