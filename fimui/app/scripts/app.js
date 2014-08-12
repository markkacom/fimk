(function () {
'use strict';

var module = angular.module('dgex.base', [
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
  'ngStorage'
]);

module.run(function ($log, accountsService, $rootScope, serverService) {
  $log.log('dgex.base application started');

  if (serverService.isNodeJS() && process) {
    process.on('exit', function () {
      console.log('NODEJS process exit');
      serverService.stopServer();
    });    
  }
});

})();