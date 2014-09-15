(function () {
'use strict';
var module = angular.module('fim.base');
module.controller('SettingsPlugin', function($scope, plugins) {
  $scope.plugins = [];
  plugins.install('settings', function (plugin) {
    $scope.plugins.push(plugin);
  });
});
})();