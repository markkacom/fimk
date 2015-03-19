(function () {
'use strict';
var module = angular.module('fim.base');

module.config(function($routeProvider) {  
  $routeProvider
    .when('/assets/:engine/:asset/:section', {
      templateUrl: 'plugins/assets/partials/assets.html',
      controller: 'AssetsPlugin'
    });
});

module.run(function (plugins, $sce) {
  plugins.register({
    id: 'assets',
  });
});

})();