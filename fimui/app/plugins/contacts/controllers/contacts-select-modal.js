(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('ContactsPluginSelectModalController', function (items, $modalInstance, 
  $scope, $timeout, db, ngTableParams, alerts, plugins) {

  $scope.items = items;
  $scope.contacts = [];

  /* Load contacts from database */
  db.contacts.orderBy('name').toArray().then(
    function (contacts) {
      $scope.contacts = contacts;
      refresh();
    }
  ).catch(alerts.catch("Could not load contacts"));  

  /* Register CRUD observer for contacts */
  db.contacts.addObserver($scope, 
    db.createObserver($scope, 'contacts', 'id_rs', {
      finally: function () {
        refresh();
      }
    })
  );

  $scope.selectContact = function (id_rs) {
    $modalInstance.close({ id_rs: id_rs });
  };

  $scope.showAccount = function (id_rs) {
    plugins.get('accounts').detail(id_rs);
  };

  $scope.dismiss = function () {
    $modalInstance.dismiss();
  };

  function refresh() {
    $timeout(function () { 
      $scope.tableParams = new ngTableParams(
        { page: 1, count: 10 }, 
        { total: $scope.contacts.length,
          getData: function($defer, params) {
            $defer.resolve($scope.contacts.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
        }
      );
    });
  }
});

})();