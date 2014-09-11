(function () {
'use strict';

var module = angular.module('fim.base');

module.factory('alerts', function ($rootScope) {
  return {
    catch: function(message){
      return function(reason){
        console.error('alerts.catch', message + ' ' + reason);
        $rootScope.alert.failed(message + " " + reason);
      };
    },
    success: function (message) {
      $rootScope.alert.success(message);
    },
    failed: function (message) {
      console.error('alerts.failed', message);
      $rootScope.alert.failed(message);
    }
  };
});

})();