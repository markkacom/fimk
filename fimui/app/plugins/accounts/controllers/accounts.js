(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('AccountsPlugin', function($state, $q, $rootScope, 
  $scope, modals, $stateParams, $location, nxt, $timeout, db, 
  $log, alerts, plugins) {

  $scope.accounts        = [];
  $scope.selectedAccount = null;
  $scope.plugins         = { create: [], open: [] };

  /* Install plugins */
  plugins.install('accounts', function (plugin) {
    if (plugin.create) {
      $scope.plugins.create.push(plugin);
    }
    else if (plugin.open) {
      $scope.plugins.open.push(plugin);
    }
  });

  /* Load accounts from database */
  db.accounts.orderBy('name').toArray().then(
    function (accounts) {
      $timeout(function () { 
        $scope.accounts = accounts;
        fixlocation();
        refresh();
      });
    }
  ).catch(alerts.catch("Could not load accounts"));  

  /* Register CRUD observer for accounts */
  db.accounts.addObserver(db.createObserver($scope, 'accounts', 'id_rs'), $scope, {
    finally: function () {
      fixlocation();
    }
  });

  $scope.updateSelection = function () {
    $state.go('accounts', {id_rs: $scope.selectedAccount.id_rs});
  }

  /* Find an account by id_rs */
  $scope.findAccount = function (id_rs) {
    return UTILS.findFirst($scope.accounts, function (account) {
      return account.id_rs === id_rs;
    });
  };

  /* Show the add account modal dialog */
  $scope.addAccount = function () {
    var account = {};
    function run() {
      modals.open('accountsAdd', {
        resolve: {
          items: function () {
            return account;
          }
        },
        close: function (items) {
          console.log('modal-add close', items);
          db.accounts.add(items).then(
            function () {
              alerts.success("Account added");
            }
          ).catch(
            function (error) {
              account = items;
              alerts.failed("Account add failed");
              run();
            }
          );
        }
      });
    }
    run();
  };  

  /* Show the remove account modal dialog */
  $scope.removeAccount = function (account) {
    modals.open('warn', {
      resolve: {
        items: function () {
          return {
            title: 'Delete Account',
            message: 'Are you sure you want to remove this account?'
          };
        }
      },
      close: function (items) {
        account.delete().then(
          function () {
            if ($scope.selectedAccount && account.id_rs === $scope.selectedAccount.id_rs) {
              if ($scope.accounts.length > 0) {
                $state.go('accounts', {id_rs: $scope.accounts[0].id_rs});
              }
              else {
                $state.go('accounts');
              }
            }
          }
        );
      }
    });
  };

  /* Show the edit account modal dialog */
  $scope.editAccount = function (account) {
    modals.open('accountsEdit', {
      resolve: {
        items: function () {
          return {
            name: account.name,
            id_rs: account.id_rs
          };
        }
      },
      close: function (items) {
        account.update(items);
      }
    });
  };

  /* Called once the database returned the accounts - selects account based on URI or fixes URI */
  function fixlocation() {
    console.log('fixlocation', $scope.selectedAccount);
    /* URL contains account ID - make that account the selected account */
    var selected = $scope.accounts[UTILS.findFirstPropIndex($scope.accounts, $stateParams, 'id_rs', 'id_rs')];
    if (selected) {
      // angular.forEach($scope.accounts, function (a) { a.active = false; });
      // selected.active = true;
      $scope.selectedAccount = selected;
    }
    /* URL is empty /#/accounts/ */
    else if (!$stateParams.id_rs || $stateParams.id_rs.trim().length == 0) {
      if ($scope.accounts.length > 0) {
        $state.go('accounts', {id_rs: $scope.accounts[0].id_rs});
      }      
    }
    /* URL is not empty but that account is not in the database */
    else if ($scope.accounts.length > 0) {
      $state.go('accounts', {id_rs: $scope.accounts[0].id_rs});
    }
    else {
      $state.go('accounts');
    }
  }  

  /* Used as update interval and when selectedAccount changes - updates the database with info from the server */
  function refresh() {
    if ($scope.selectedAccount === null) return;
    var selected = $scope.selectedAccount;

    /* Fetch user balance */
    nxt.getBalance({ account: selected.id_rs }).then(
      function (data) {
        $timeout(function () {
          selected.update({
            guaranteedBalanceNXT: NRS.convertToNXT(data.guaranteedBalanceNQT),
            balanceNXT: NRS.convertToNXT(data.balanceNQT),
            effectiveBalanceNXT: data.effectiveBalanceNXT,
            unconfirmedBalanceNXT: NRS.convertToNXT(data.unconfirmedBalanceNQT),
            forgedBalanceNXT: NRS.convertToNXT(data.forgedBalanceNQT)
          });
        });
      },
      alerts.catch("Could not get balance")
    );

    /* Fetch user transactions */
    nxt.getAccountTransactions({ account: selected.id_rs }).then(
      function (transactions) {
        db.transaction("rw", db.transactions, function() {
          angular.forEach(transactions, function (transaction) {
            db.transactions.put(transaction);
          });
        });
      },
      alerts.catch("Could not get transactions")
    );
  }

});

})();