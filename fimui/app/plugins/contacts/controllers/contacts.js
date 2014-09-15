(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('ContactsPlugin', function($state, $scope, plugins, alerts) {
  $scope.addContact = function () {
    plugins.get('contacts').add({
      message: 'Please enter the details for this contact',
      id_rs: ''
    }).then(
      function () {
        alerts.success('Successfully added contact');
      }
    ).catch(alerts.catch('Could not safe contact'));
  };
});

})();