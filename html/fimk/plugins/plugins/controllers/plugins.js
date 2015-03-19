(function () {
'use strict';
var module = angular.module('fim.base');
module.controller('PluginsPlugin', function($scope, plugins, $routeParams) {
  $scope.plugins = [];
  var plugin_id  = {};

  plugins.install('plugins', function (plugin) {
    $scope.plugins.push(plugin);
    plugin_id[plugin.id] = plugin;
  });

  $scope.selectedPlugin = plugin_id[$routeParams.id];
});
})();