(function () {
'use strict';

var module = angular.module('dgex.base');

module.factory('accountsService', function($rootScope, $http, $q, $log, $localStorage, $sessionStorage) {

var id_key = '_fimk_id';
var supported_properties = ['name', 'account_id', 'account_id_rs'];
var localStorage = $localStorage.$default({ 
  accounts: [],
  transactions: {},
  blocks: {}
});
var sessionStorage = $sessionStorage.$default({ 
  account_keys: {} 
});

/* Returns a unique id, not used on any of the account objects */
function getUniqueId() {
  var id = 0;
  var existing = [];
  UTILS.each(localStorage.accounts, function (account) {
    existing.push(account[id_key]);
  });
  while (existing.indexOf(id) != -1) {
    id++;
  }
  return id;
}

return {

  accounts: localStorage.accounts,
  transactions: localStorage.transactions,
  blocks: localStorage.blocks,

  add: function add(account) {
    localStorage.accounts.push(account);
    $rootScope.$emit('accounts.add');
  },
 
  remove: function remove(account) {
    if (!UTILS.removeFirst(localStorage.accounts, function (_account) { return account[id_key] == _account[id_key] })) {
      throw new Error('Could not find account: ' + account)
    }
    $rootScope.$emit('accounts.remove');
  },
 
  create: function create(options) {
    var account = {};
    account[id_key] = getUniqueId();
    account['name'] = options.name;
    account.transaction_ids = [];
    account.transactions = [];

    angular.forEach(localStorage.accounts, function (_account) {
      if (_account.name == account.name) { throw new Error('Duplicate name') }
      if (_account[id_key] == account[id_key]) { throw new Error('Duplicate unique key') }
    });

    for (var name in options) {
      if (supported_properties.indexOf(name) >= 0) { account[name] = options[name] }
      else { throw new Error('Unsupported property: ' + name) }
    }
    return account;
  },

  findFirstBy: function findFirstBy(name, value) {
   return UTILS.findFirst(localStorage.accounts, function (_account) {
      return _account[name] == value;
    });
  },

  unlock: function (account, secretPhrase) {
    sessionStorage.account_keys[account[id_key]] = secretPhrase;
  },

  lock: function (account) {
    delete sessionStorage.account_keys[account[id_key]];
  }
};

});

})();