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

module.run(function (modals, plugins) {
  
  /* Register as plugin */
  plugins.register({
    id: 'accounts',
    extends: 'app',
    sref: 'accounts',
    label: 'Accounts'
  });

  /* Register modal dialogs */
  modals.register('accountsAdd', { 
    templateUrl: 'plugins/accounts/partials/accounts-add-modal.html', 
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