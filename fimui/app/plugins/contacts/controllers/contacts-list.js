(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('ContactsPluginListController', function($scope, alerts, $timeout, ngTableParams, db, plugins) {

  $scope.contacts = [];
  $scope.tableParams = new ngTableParams({
      page: 1, 
      count: 20
    }, 
    {
      total: 0, 
      getData: function($defer, params) {
        $defer.resolve($scope.contacts.slice((params.page() - 1) * params.count(), params.page() * params.count()));
      }
    }
  );

  /* Load contacts from database */
  db.contacts.orderBy('name').toArray().then(
    function (contacts) {
      $timeout(function () {
        $scope.contacts = contacts;
        $scope.tableParams.total($scope.contacts);
        $scope.tableParams.reload();
      });
    }
  ).catch(alerts.catch("Could not load contacts"));  

  /* Register CRUD observer for contacts */
  db.contacts.addObserver($scope, 
    db.createObserver($scope, 'contacts', 'id_rs', {
      finally: function () {
        $scope.tableParams.total($scope.contacts);
        $scope.tableParams.reload();
      }
    })
  );

  function find(id_rs) {
    for (var i=0; i<$scope.contacts.length; i++) {
      if ($scope.contacts[i].id_rs == id_rs) {
        return $scope.contacts[i];
      }
    }
  }

  $scope.showAccount = function (id_rs) {
    plugins.get('accounts').detail(id_rs);
  };

  $scope.editContact = function (contact) {
    var args = {
      message: 'Please enter the details for this contact',
    };
    args.id_rs    = contact.id_rs;
    args.name     = contact.name;
    args.email    = contact.email || '';
    args.website  = contact.website || '';

    plugins.get('contacts').update(args).then(
      function () {
        alerts.success('Successfully updated contact');
      }
    ).catch(alerts.catch('Could not update contact'));
  };

  $scope.removeContact = function (contact) {
    if (window.confirm('Are you sure you want to delete this contact?')) {
      db.contacts.delete(contact.id_rs).catch(alerts.catch('Could not delete contact'));
    }
  };

  $scope.sendMoney = function (contact) {
    plugins.get('payment').create({
      recipientRS:        contact.id_rs,
      recipientReadonly:  true
    });
  };

});
})();