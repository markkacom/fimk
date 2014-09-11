(function () {
'use strict';

var module = angular.module('fim.base');

module.factory('serverService', function ($modal, $http, $q, modals) {

var must_use_post = true;
var canceller     = $q.defer();

/* Dirty hack to detect if we are on https:wallet.fimk.fi */
if (window.location.protocol.indexOf('https') == 0) {
  var host        = window.location.protocol + '//' + window.location.host;
}
else {
  /*var host        = 'http://127.0.0.1:7886'*/
  var host        = 'http://178.62.176.45:6886'; /* TEST NET develop-1.2.6 */
}

var requestTypes  = {
  broadcastTransaction: { requirePost: true },
  calculateFullHash: {},
  cancelAskOrder: { requirePost: true, requireSecret: true },
  cancelBidOrder: { requirePost: true, requireSecret: true },
  castVote: { requirePost: true, requireSecret: true },
  createPoll: { requirePost: true, requireSecret: true },
  decodeHallmark: {},
  decodeToken: {},
  generateToken: { requirePost: true, requireSecret: true },
  getAccount: {},
  getAccountBlockIds: {},
  getAccountId: { requirePost: true, requireSecret: true },
  getAccountPublicKey: {},
  getAccountTransactionIds: {},
  getAccountTransactions: {},
  getAlias: {},
  getAliases: {},
  getAllAssets: {},
  getAsset: {},
  getAssets: {},
  getAssetIds: {},
  getAssetsByIssuer: {},
  getBalance: { requiredArgs: ['account'] },
  getBlock: {},
  getBlockchainStatus: {},
  getBlocksIdsFromHeight: {},
  getConstants: {},
  getGuaranteedBalance: {},
  getMyInfo: {},
  getNextBlockGenerators: {},
  getPeer: {},
  getPeers: {},
  getPoll: {},
  getPollIds: {},
  getState: {},
  getTime: {},
  getTrades: {},
  getAllTrades: {},
  getTransaction: {},
  getTransactionBytes: {},
  getUnconfirmedTransactionIds: {},
  getUnconfirmedTransactions: {},
  getAccountCurrentAskOrderIds: {},
  getAccountCurrentBidOrderIds: {},
  getAllOpenOrders: {},
  getAskOrder: {},
  getAskOrderIds: {},
  getAskOrders: {},
  getBidOrder: {},
  getBidOrderIds: {},
  getBidOrders: {},
  issueAsset: { requirePost: true, requireSecret: true },
  leaseBalance: { requirePost: true, requireSecret: true },
  markHost: { requirePost: true, requireSecret: true },
  parseTransaction: {},
  placeAskOrder: {},
  placeBidOrder: {},
  sendMessage: { requirePost: true, requireSecret: true },
  sendMoney: { requirePost: true, requireSecret: true },
  setAccountInfo: { requirePost: true, requireSecret: true },
  setAlias: { requirePost: true, requireSecret: true },
  signTransaction: {},
  startForging: { requirePost: true, requireSecret: true },
  stopForging: { requirePost: true, requireSecret: true },
  getForging: { requirePost: true, requireSecret: true },
  transferAsset: {}
};

function cancel_requests() {
  canceller.resolve();
  canceller = $q.defer();
}

function create_url(requestType) {
  return host + '/nxt?requestType=' + requestType + '&random=' + Math.random();
}

function do_post(requestType, args) {
  var qs = "";
  if (Array.isArray(args)) {
    angular.forEach(args, function (tuple) {
      for (var name in tuple) {
        qs += '&' + name + '=' + encodeURIComponent(tuple[name]);
      }    
    });
  }
  else {
    for (var name in args) {
      qs += '&' + name + '=' + encodeURIComponent(args[name]);
    }    
  }
  return $http({
    method: 'POST',
    dataType: 'json',
    url: create_url(requestType),
    data: qs,
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    timeout: canceller.promise
  });
}

function do_get(requestType, args) {
  var url = create_url(requestType);
  if (Array.isArray(args)) {
    angular.forEach(args, function (tuple) {
      for (var name in tuple) {
        url += '&' + name + '=' + encodeURIComponent(tuple[name]);
      }    
    });
  }
  else {
    for (var name in args) {
      url += '&' + name + '=' + encodeURIComponent(args[name]);
    }    
  }
  return $http({ 
    method: 'GET', 
    dataType: 'json',
    url: url,
    timeout: canceller.promise 
  });
}

/* Prepares a request prompts the user for a secretPhrase before continueing */
function prepare_request(requestType, args) {
  var details = requestTypes[requestType];
  if (!details) {
    throw new Error('Unsupported requestType: ' + requestType);
  }

  if (details.requiredArgs) {
    for (var i=0; i<details.requiredArgs.length; i++) {
      if (!(details.requiredArgs[i] in args)) {
        throw new Error('Missing required argument: ' + name);
      }
    }
  }

  var method = details.requirePost && must_use_post ? 'POST' : 'GET';
  var deferred = $q.defer();
  if (details.requireSecret && !('secretPhrase' in args)) {
    modals.open('secretPhrase', {
      resolve: {
        items: function () {
          return {
            secretPhrase: '',
            account: args.account
          };
        }
      },
      close: function (items) {
        args.secretPhrase = items.secretPhrase;
        deferred.resolve({ method: method, args: args });
      },
      cancel: function (items) {
        deferred.reject();
      }
    });
  }
  else {
    deferred.resolve({ method: method, args: args });
  }
  return deferred.promise;
}

// -------------------------------------------------------------------------
// Start and Stop Java Server
// -------------------------------------------------------------------------

var cross_platform  = {
  WIN: {
    start: {
      command: 'java',
      args: ['-cp', "fim.jar;lib\\*;conf", 'nxt.Nxt']
    }
  },
  LINUX: {
    start: {
      command: 'java',
      args: ['-cp', 'fim.jar:lib/*:conf', 'nxt.Nxt']
    }
  }
};
cross_platform.MAC = cross_platform.LINUX;

function getOS() {
  if (navigator.appVersion.indexOf("Win")!=-1) return 'WIN';
  if (navigator.appVersion.indexOf("Mac")!=-1) return 'MAC';
  if (navigator.appVersion.indexOf("X11")!=-1) return 'LINUX';
  if (navigator.appVersion.indexOf("Linux")!=-1) return 'LINUX';
  throw new Error('Could not detect OS');
}

return {
  server: null,
  listeners: { stdout: [], stderr: [], exit: [], start: [] },
  messages: [],

  sendRequest: function (requestType, args, qs) {
    var deferred = $q.defer();
    prepare_request(requestType, args, qs).then(
      function (request_data) {
        console.log(request_data);
        var promise = request_data.method == 'POST' ? 
          do_post(requestType, request_data.args, qs) : do_get(requestType, request_data.args, qs);
        promise.then(
          function (data) {
            if (data.data.errorCode && !data.data.errorDescription) {
              data.data.errorDescription = (data.data.errorMessage ? data.data.errorMessage : "Unknown error occured.");
            }
            if (data.data.errorDescription) {
              deferred.reject(data.data);
              return;
            }
            deferred.resolve(data.data);
          }, 
          function (error) {
            deferred.reject(error);
          }
        );
      }, 
      function cancel(error) {
        deferred.reject('cancel');
      }
    );
    return deferred.promise;
  },
  isNodeJS: function () {
    return this._isNodeJS || (this._isNodeJS = (typeof require == 'function' && require('child_process')));
  },
  startServer: function () {
    var path      = require('path')
    var parentDir = path.dirname( process.execPath );
    var start_options = {  cwd: parentDir };

    console.log('parentDir: ' + parentDir);

    var os      = getOS();
    var spawn   = require('child_process').spawn;
    this.server = spawn(cross_platform[os].start.command, cross_platform[os].start.args, start_options);

    var self    = this;
    this.server.stdout.on('data', function (data) {
      console.log(data + '');
      self.messages.push(data.toString());
      self.notifyListeners('stdout', data);
    });

    this.server.stderr.on('data', function (data) {
      console.log(data + '');
      self.messages.push(data.toString());
      self.notifyListeners('stderr', data);
    });

    this.server.on('exit', function (code) {
      console.log('Server Exit!');
      self.notifyListeners('exit', code);
    });

    this.notifyListeners('start', this.server);
  },
  stopServer: function () {
    if (this.server) {
      this.server.kill();
      this.server = null;
    }
  },
  addListener: function (type, listener) {
    this.listeners[type].push(listener);
  },  
  notifyListeners: function (type, data) {
    angular.forEach(this.listeners[type], function (func) {
      func(data);
    })
  },
  getMessages: function () {
    return this.messages;
  }
};

});

})();