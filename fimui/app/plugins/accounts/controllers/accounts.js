(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('AccountsPlugin', function($state, $q, $rootScope, 
  $scope, modals, $stateParams, $location, nxt, $timeout, db, 
  $log, alerts, plugins) {

  $scope.accounts        = [];
  $scope.selectedAccount = null;
  $scope.plugins         = { create: [], open: [] };
  $scope.errorCode       = null;
  $scope.depositPublishAddress = null;

  /* Poll for new transactions every 10 seconds */
  var interval = setInterval(function interval() { $scope.refresh() }, 10 * 1000);
  $scope.$on("$destroy", function() { clearInterval(interval) });

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
      });
    }
  ).catch(alerts.catch("Could not load accounts"));  

  /* Register CRUD observer for accounts */
  db.accounts.addObserver($scope, 
    db.createObserver($scope, 'accounts', 'id_rs', {
      finally: function () {
        fixlocation(); /* if the selectedAccount is removed from the db this triggers a page reload */
      }
    })
  );
 
  /* account select ng-change */
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
    plugins.get('accounts').add(account).then(
      function (items) {
        console.log('accounts.addAccount', items);
        $state.go('accounts', {id_rs: items.id_rs});
      }
    );
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
            $state.go('accounts');
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
        $timeout(function () {
          account.update(items);
        });        
      }
    });
  };

  $scope.publishAndFundAccount = function () {
    plugins.get('payment').create(
      {
        recipientRS: $scope.selectedAccount.id_rs,
        recipientPublicKey: $scope.selectedAccount.publicKey,
        feeNXT: '0.1',
        amountNXT: '1',
        recipientReadonly: true,
        skipSaveContact: true,
        message: 'By sending FIM to your new address you will publish the publickey.'
      }
    ).then( 
      function (items) {
        if (items.transaction) {
          alerts.success('Successfully published public key. Please wait for network to process.')
        }
      }
    );
  };

  /* Called once the database returned the accounts - selects account based on URI or fixes URI */
  function fixlocation() {
    /* URL contains account ID - make that account the selected account */
    var selected = $scope.accounts[UTILS.findFirstPropIndex($scope.accounts, $stateParams, 'id_rs', 'id_rs')];
    if (selected) {
      var changed = (selected !== $scope.selectedAccount);
      $scope.selectedAccount = selected;
      if (changed) {
        $scope.refresh();
      }
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
  $scope.refresh = function () {
    if ($scope.selectedAccount === null) return;
    var selected = $scope.selectedAccount;

    /* Fetch account info */
    nxt.getAccount({ account: selected.id_rs }).then(
      function (data) {
        $timeout(function () {
          selected.update({
            guaranteedBalanceNXT: nxt.util.convertToNXT(data.guaranteedBalanceNQT),
            balanceNXT: nxt.util.convertToNXT(data.balanceNQT),
            effectiveBalanceNXT: nxt.util.commaFormat(String(data.effectiveBalanceNXT)),
            unconfirmedBalanceNXT: nxt.util.convertToNXT(data.unconfirmedBalanceNQT),
            forgedBalanceNXT: nxt.util.convertToNXT(data.forgedBalanceNQT),
            publicKey: data.publicKey,
            id: data.account
          });
          if (!data.publicKey) {
            $scope.errorCode = 5;
            $scope.depositPublishAddress = selected.id_rs+':'+selected.publicKey;            
          }
        });
      },
      function (error) {
        if (error.errorCode == 5) {
          $timeout(function () {
            $scope.errorCode = 5;
            $scope.depositPublishAddress = selected.id_rs+':'+selected.publicKey;
          });
        }
        else {
          alerts.failed(error.errorDescription);
        }
      }
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
      function (error) {
        if (error.errorCode != 5) {
          alerts.failed("Could not get transactions");
        }
      }
    );
  }

});

})();