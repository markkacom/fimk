(function () {
'use strict';

var module = angular.module('fim.base', [
  'ngAnimate',
  'ngCookies',
  'ngResource',
  'ui.router',
  'ui.bootstrap',
  'ngSanitize',
  'ngTouch',
  'angular-loading-bar',  
  'ngGrid',
  'ngCookies',
  'pascalprecht.translate',
  'ngTable'
]);

module.run(function ($log, $rootScope, serverService) {
  $log.log('fim.base application started');

  if (serverService.isNodeJS() && process) {
    process.on('exit', function () {
      console.log('NODEJS process exit');
      serverService.stopServer();
    });    
  }
});

})();