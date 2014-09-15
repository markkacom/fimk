(function () {
'use strict';
var module = angular.module('fim.base');
module.config(function($stateProvider) {  
  $stateProvider.
    state('settings', {
      url: '/settings',
      templateUrl: 'plugins/settings/partials/settings.html',
      controller: 'SettingsPlugin'
    });
});

module.run(function (plugins) {  

  /* Register as plugin */
  plugins.register({
    id: 'settings',
    extends: 'app',
    sref: 'settings',
    label: 'Settings'
  });

});

})();