(function () {
'use strict';
var module = angular.module('fim.base');
module.config(function($routeProvider) {  
  $routeProvider
    .when('/activity/:engine/:section/:period', {
      templateUrl: 'plugins/activity/partials/activity.html',
      controller: 'ActivityPlugin'
    });
});

module.run(function (plugins, $sce) {
  plugins.register({
    id: 'activity',
  });
});
})();