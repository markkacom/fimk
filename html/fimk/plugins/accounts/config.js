(function () {
'use strict';
var module = angular.module('fim.base');
module.config(function($routeProvider) {
  $routeProvider
    .when('/accounts/:id_rs/:section/:period', {
      templateUrl: 'plugins/accounts/partials/accounts.html',
      controller: 'AccountsPlugin'
    });
});

module.run(function (modals, plugins, nxt, alerts, $q, db, $timeout, $sce) {

  /* Register as plugin */
  plugins.register({
    id: 'accounts',

    detail: function (id_rs) {
      nxt.get(id_rs).getAccount({account: id_rs}).then(
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
    /* depracated */
    add: function (api) {
      var deferred = $q.defer();
      modals.open('accountsAdd', {
        resolve: {
          items: function () {
            return { api: api };
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
    },

    loadAccounts: function () {
      var deferred = $q.defer();
      loadAccounts().then(deferred.resolve, deferred.reject);
      return deferred.promise;
    }
  });

  /* Register modal dialogs */
  modals.register('sendProgress', { 
    templateUrl: 'plugins/accounts/partials/send-progress.html', 
    controller: 'AccountsPluginSendProgressMoneyModalController' 
  });
});

})();