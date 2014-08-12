(function () {
'use strict';
var module = angular.module('dgex.base');

module.controller('accountsController', function($state, $q, $rootScope, $scope, accountsService, modals, $stateParams, $location, serverService, $timeout) {

  $rootScope.$on('accounts.add', function () {
    $timeout(function () {
      $('#a-' + accountsService.accounts[0].account_id_rs).click();
    });
  });

  $rootScope.$on('accounts.remove', function () {
    $timeout(function () {
      try { $('#a-' + accountsService.accounts[0].account_id_rs).click() } catch (e) {}
    }); 
  });

  $scope.accounts = accountsService.accounts;
  $scope.selectedAccount = null;

  function setSelectedAccount(account) {
    angular.forEach($scope.accounts, function (_account) {
      _account.active = false;
    });
    account.active = true;
    $scope.selectedAccount = account;
  }

  if (!$stateParams.account_id_rs || $stateParams.account_id_rs.trim().length == 0) {
    if ($scope.accounts.length > 0) {
      $location.path('/accounts/' + $scope.accounts[0].account_id_rs);
      return;
    }
  }
  else {
    var selected_account = UTILS.findFirst($scope.accounts, function (_account) { return _account.account_id_rs == $stateParams.account_id_rs });
    if (selected_account) {
      setSelectedAccount(selected_account);
    }
    else {
      $location.path('/accounts/');
      return;
    }
  }

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
        $scope.selectedAccount = null;
        accountsService.remove(account);
      },
      cancel: function (items) {        
      }
    });        
  };

  $scope.addAccount = function () {
    modals.open('accountsAdd', {
      resolve: {
        items: function () {
          return {
            name: '',
            passphrase: '',
            account_id: '', 
            account_id_rs: ''
          };
        }
      },
      close: function (items) {
        console.log(items)
        var _account = accountsService.create(items);
        accountsService.add(_account);
      },
      cancel: function (items) {        
      }
    });    
  }

  function getBalance() {
    if ($scope.selectedAccount) {
      serverService.sendRequest('getBalance', { account: $scope.selectedAccount.account_id_rs }).then(
        function (data) {
          console.log(data);
          $scope.selectedAccount.guaranteedBalanceNQT = data.guaranteedBalanceNQT;
          $scope.selectedAccount.balanceNQT = data.balanceNQT;
          $scope.selectedAccount.effectiveBalanceNXT = data.effectiveBalanceNXT;
          $scope.selectedAccount.unconfirmedBalanceNQT = data.unconfirmedBalanceNQT;
          $scope.selectedAccount.forgedBalanceNQT = data.forgedBalanceNQT;
        },
        function (error) {

        }
      );
    }
  }

  function getTransactions() {
    if ($scope.selectedAccount) {    
      var timestamp = $scope.selectedAccount.lastTransactionsTimestamp || 0;
      serverService.sendRequest('getAccountTransactionIds', { 
        account: $scope.selectedAccount.account_id_rs, 
        timestamp: timestamp 
      }).then(
        function (data) {
          if (data.transactionIds && data.transactionIds.length) {
            for (var i = 0; i < data.transactionIds.length; i++) {
              var id = data.transactionIds[i];
              if (accountsService.transactions[id]) {
                if ($scope.selectedAccount.transaction_ids.indexOf(id) == -1) {
                  $scope.selectedAccount.transaction_ids.push(id);
                }                
              }
              else {
                serverService.sendRequest('getTransaction', { 
                  transaction: id 
                }).then( 
                  function (transaction) {
                    console.log(transaction);
                    accountsService.transactions[transaction.transaction] = transaction;
                    if ($scope.selectedAccount.transaction_ids.indexOf(transaction.transaction) == -1) {
                      $scope.selectedAccount.transaction_ids.push(transaction.transaction);
                    }
                  },
                  function (error) {}
                );
              }
            }
          }
        },
        function (error) {

        }
      );
    }
  }

  getBalance();
  getTransactions();
});

})();