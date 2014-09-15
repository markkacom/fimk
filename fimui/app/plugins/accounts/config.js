(function () {
'use strict';
var module = angular.module('fim.base');
module.config(function($stateProvider) {  
  $stateProvider.state('accounts', {
    url: '/accounts/:id_rs',
    views: {
      '': { 
        templateUrl: 'plugins/accounts/partials/accounts.html',
        controller: 'AccountsPlugin'
      },
      'transactions@accounts': {
        templateUrl: 'plugins/accounts/partials/accounts-transactions.html',
        controller: 'AccountsPluginTransactionsController'
      }           
    }
  });
});

module.run(function (modals, plugins, nxt, alerts, $q) {
  
  /* Register as plugin */
  plugins.register({
    id: 'accounts',
    extends: 'app',
    sref: 'accounts',
    label: 'Accounts',
    detail: function (id_rs) {
      nxt.getAccount({account: id_rs}).then(
        function (account) {
          modals.open('accountsDetail', {
            resolve: {
              items: function () {
                angular.extend(account, {
                  guaranteedBalanceNXT:   nxt.util.convertToNXT(account.guaranteedBalanceNQT),
                  balanceNXT:             nxt.util.convertToNXT(account.balanceNQT),
                  effectiveBalanceNXT:    nxt.util.commaFormat(String(account.effectiveBalanceNXT)),
                  unconfirmedBalanceNXT:  nxt.util.convertToNXT(account.unconfirmedBalanceNQT),
                  forgedBalanceNXT:       nxt.util.convertToNXT(account.forgedBalanceNQT)
                });
                return account;
              }
            }
          });
        },
        alerts.catch("Could not obtain account")
      );      
    },
    add: function (args) {
      var deferred = $q.defer();
      modals.open('accountsAdd', {
        resolve: {
          items: function () {
            return args;
          }
        },
        close: function (items) {
          deferred.resolve(items);
        },
        cancel: function () {
          deferred.reject();
        }
      });
      return deferred.promise
    }
  });

  /* Register modal dialogs */
  modals.register('accountsAdd', { 
    templateUrl: 'plugins/accounts/partials/accounts-add-modal2.html', 
    controller: 'AccountsPluginAddModalController' 
  });
  modals.register('accountsDetail', { 
    templateUrl: 'plugins/accounts/partials/accounts-detail-modal.html', 
    controller: 'AccountsPluginDetailModalController' 
  });
  modals.register('accountsEdit', { 
    templateUrl: 'plugins/accounts/partials/accounts-edit-modal.html', 
    controller: 'AccountsPluginEditModalController' 
  });
});

})();