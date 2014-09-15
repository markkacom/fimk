(function () {
'use strict';

var uriParser = null;
var module = angular.module('fim.base');

module.controller('appController', function($rootScope, $scope, $modal, $q, $log,  
  $timeout, modals, $window, plugins, alerts, serverService, db, settings) {

  /* Install app plugins */
  $scope.plugins = [];
  plugins.install('app', function (plugin) {
    console.log('install', plugin)
    $scope.plugins.push(plugin);
  });

  /* Install theme switcher plugins */
  $scope.themes = [];
  angular.forEach(plugins.get('themes').registry, function (config) {
    $scope.themes.push(config);
  });
  $scope.selectedTheme = plugins.get('themes').default();

  function getThemeById(id) {
    for (var i=0; i<$scope.themes.length; i++) {
      if ($scope.themes[i].id == id) {
        return $scope.themes[i];
      }
    }
  }

  /* Register default theme settings */
  settings.initialize([{
    id: 'themes.default.theme',
    value: 'default',
    type: String,
    label: 'Default theme',
    resolve: function (value) {
      console.log('themes.resolve', value)
      $scope.selectedTheme = getThemeById(value);
    }
  }]);


  $scope.themeChanged = function () {
    console.log('themeChanged.selectedTheme', $scope.selectedTheme);
    settings.update('themes.default.theme', $scope.selectedTheme.id);
  };

  $scope.alerts   = [];
  $scope.balances = [];
  $scope.isNodeJS = serverService.isNodeJS();

  $rootScope.$on('$stateChangeError', function(event, toState, toParams, fromState, fromParams, error){
    $log.warn('STATE CHANGE ERROR');
    $log.error(event);
  });

  /* Listens for window onresize and sets the outer container to a fixed height */
  var w = angular.element($window);
  w.bind('resize', function () {
    $scope.$apply();
  });
  $scope.getOuterContainerStyle = function () {
    return (w.width() > 800) ? { height: ((w.height() - 100) + 'px') } : {};
  }

  function parseURI(uri) {
    uriParser = uriParser || document.createElement('a');
    uriParser.href = uri;
    return uriParser;
  }

  $rootScope.canceller = $q.defer();

  $rootScope.cancelAllRequests = function () {
    $scope.alerts = [];
    $rootScope.canceller.resolve();
    $rootScope.canceller = $q.defer();
  };
 
  $rootScope.alert = {
    failed: function (error) {
      error = typeof error === 'string' ? { msg: error } : error;
      var id = UTILS.uniqueID();
      var data = { type: 'danger', msg: error.msg, url: parseURI(error.url).host, retry: error.retry, id:id };      
      $scope.alerts.push(data);
      $timeout(function () { $rootScope.closeAlert(id); }, 5000);
    },
    success: function (msg) {
      var id = UTILS.uniqueID();
      var data = { type: 'success', msg: msg, id:id };      
      $scope.alerts.push(data);
      $timeout(function () { $rootScope.closeAlert(id); }, 5000);
    }
  };

  $rootScope.closeAlert = function(id) {
    UTILS.removeFirst($scope.alerts, function (data) { 
      return data.id == id 
    });
  };

  $scope.openDevTools = function () {
    try { 
      require('nw.gui').Window.get().showDevTools()  
    } catch (e) {
      console.log(e)
    }
  }

});

})();
