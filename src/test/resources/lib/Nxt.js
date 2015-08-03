var APICall = Java.type('nxt.http.APICall');
var BlockchainTest = Java.type('nxt.BlockchainTest');
var Nxt = {};
var navigator = {}; // jsbn.js dependency

load("src/test/resources/lib/jsbn.js");
load("src/test/resources/lib/jsbn2.js");
load("src/test/resources/lib/nxtaddress.js");
load("src/test/resources/lib/converters.js");
load("src/test/resources/lib/curve25519_.js");
load("src/test/resources/lib/curve25519.js");
load("src/test/resources/lib/jssha256.js");
load("src/test/resources/lib/Nxt.util.js");

var forgerSecretPhrase = "franz dark offer race fuel fake joust waste tensor jk sw 101st"; // FIM-9MAB-AXFN-XXL7-6BHU3
var secretPhrase1 = "anion harp ere sandal cobol chink bunch tire clare power fogy hump"; // FIM-R4X4-KMHT-RCXD-CLGFZ
var secretPhrase2 = "astral void larkin era beebe r6 guyana woke hoc dacca cancer await"; // FIM-7MGS-PLDG-ULV4-GHEHB
var secretPhrase3 = "mush ripen wharf tub shut nine baldy sk wink epsom batik 6u"; // FIM-B842-25LP-FLG8-HLTDH
var secretPhrase4 = "dublin janus spout lykes tacky gland nice bigot rubric 4v vb peace"; // FIM-LJ94-SPJZ-QHQM-B3VFF

function Step(requestType, argv) {
  this.requestType = requestType;
  this.argv = argv;
}
Step.prototype.execute = function () {
  var builder = APICall.call(this.requestType);
  var keys = Object.getOwnPropertyNames(this.argv||{});
  for (var i=0; i<keys.length; i++) {
    builder.param(keys[i], String(this.argv[keys[i]]));
  }
  if (Nxt.verbose) {
    console.log('>>> '+this.requestType+'('+JSON.stringify(this.argv)+')');
  }
  var ret = this.ret = JSON.parse(builder.build().invoke());
  if (Nxt.verbose) {
    console.log('    '+JSON.stringify(ret, null, '    '));
  }
  return ret;
}
Step.prototype.toString = function () {
    return JSON.stringify({
        requestType: this.requestType,
        argv: this.argv,
        ret: this.ret 
    });
}

function SocketEvent(event, prettyPrint) {
  this.data = event[2];
  this.topic = event[1];
  this.prettyPrint = prettyPrint;
  extend(this, event[2]);
}
SocketEvent.prototype.toString = function () {
  var str = "SOCKETEVENT - " + this.topic + " - ";
  return this.prettyPrint ? str + JSON.stringify(this.data, null, '  ') : str + JSON.stringify(this.data);
}

Nxt.enableAutoForge = true;
Nxt.verbose = false;
Nxt.steps = [];
Nxt.call = function (requestType, argv) {
  var step = new Step(requestType, argv);
  Nxt.steps.push(step);
  return step.execute();
};
Nxt.disableAutoForge = function (self, callback) {
  try {
    Nxt.enableAutoForge = false;
    callback.call(self);
  }
  finally {
    Nxt.enableAutoForge = true;
  }
}
Nxt.collectWebsocketEvents = function (self, callback) {
  try {
    Nxt.call('startCollectingWebsocketEvents', {});
    callback.call(self);
  }
  finally {
    Nxt.call('stopCollectingWebsocketEvents', {});
  }
}
Nxt.generateBlock = function () {
  ScriptHelper.generateBlock(accounts.forger.secretPhrase);
}
Nxt.rollback = function (height) {
  ScriptHelper.rollback(height);
}
Nxt.getBlockchain = function () {
  return ScriptHelper.getBlockchain();
}
Nxt.getBlockchainProcessor = function () {
  return ScriptHelper.getBlockchainProcessor();
}
Nxt.createFundedAccount = function (secretPhrase, balanceNXT) {
  var account = new Account(secretPhrase);
  accounts.forger.sendMoney(account.id_rs, balanceNXT, account.publicKey);
  return account;
}
Nxt.getWebsocketEvents = function (topic, prettyPrint) {
  var ret = Nxt.call('getWebsocketEvents', {topic:topic});
  if (isError(ret)) { printError(ret); return ret; }
  var events = ret.events || [];
  var result = events.map(function (event) { return new SocketEvent(event, prettyPrint) });
  result.first = result[0];
  return result;
}
Nxt.clearWebsocketEvents = function () {
  Nxt.call('stopCollectingWebsocketEvents', {});
  Nxt.call('startCollectingWebsocketEvents', {});
}
Nxt.getAccountByIdentifier = function (identifier) {
  var ret = Nxt.call('getAccountByIdentifier', {identifier:identifier});  
  if (isError(ret)) { printError(ret); return ret; }
  return ret;
}

function isError(obj) { return obj.errorCode || obj.error || obj.errorDescription };
function printError(obj) { print(JSON.stringify(obj)); }
function extend(dst, src) {
  var names = Object.getOwnPropertyNames(src);
  names.forEach(function (n) {
    dst[n] = src[n];
  });
  return dst;
}

function Account(secretPhrase) {
  this.secretPhrase=secretPhrase;
  this.id_rs=Nxt.util.getAccountId(secretPhrase, true);
  this.publicKey = Nxt.util.secretPhraseToPublicKey(secretPhrase);
}
Account.prototype = {
  generateBlock: function () {
    ScriptHelper.generateBlock(this.secretPhrase);
  },
  setBalanceNXT: function (balanceNXT) {
    var currentBalanceNQT = parseInt(Nxt.call('getBalance', {account: this.id_rs}).balanceNQT);
    var desiredBalanceNQT = parseInt(Nxt.util.convertToNQT(balanceNXT));
    /* current balance is higher then desired - transfer remainder to forger account */
    if (desiredBalanceNQT < currentBalanceNQT) {
      var differenceNQT = (currentBalanceNQT - desiredBalanceNQT) - parseInt(Nxt.util.convertToNQT('0.1'));
      this.sendMoney(accounts.forger.id_rs, Nxt.util.convertToNXT(differenceNQT));
    }
    /* current balance is lower then desired - transfer difference from forger account */
    else if (desiredBalanceNQT > currentBalanceNQT) {
      var differenceNQT = desiredBalanceNQT - currentBalanceNQT;
      accounts.forger.sendMoney(this.id_rs, Nxt.util.convertToNXT(differenceNQT));
    }
    assert.assertEquals(balanceNXT, this.getBalanceNXT());    
  },
  sendMoney: function (recipientRS, amountNXT, recipientPublicKey) {
    var arg = {
      secretPhrase: this.secretPhrase,
      feeNQT: Nxt.util.convertToNQT('0.1'),
      deadline: '1440',
      amountNQT: Nxt.util.convertToNQT(amountNXT)
    };
    if (typeof recipientPublicKey == 'string') { 
      arg.recipientPublicKey = recipientPublicKey; 
    }
    arg.recipient = Nxt.util.convertRSAddress(recipientRS); 
    var ret = Nxt.call('sendMoney', arg);
    if (isError(ret)) { printError(ret); return ret; }
    if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
    return ret;
  },  
  getBalanceNXT: function () {
    var ret = Nxt.call('getBalance', {account: this.id_rs});
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return null; }
    return ret.balanceNQT ? Nxt.util.convertToNXT(ret.balanceNQT) : '0';
  },
  getAssetBalance: function (asset) {
    var decimals = Nxt.call('getAsset', {asset:asset}).decimals;
    var ret = Nxt.call('getAccountAssets', {account: Nxt.util.convertRSAddress(this.id_rs), asset: asset});
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return ret; }
    return ret.quantityQNT ? Nxt.util.convertToQNTf(String(ret.quantityQNT), decimals) : '0';
  },
  transferAsset: function (recipientRS, asset, quantity) {
    var decimals = Nxt.call('getAsset', {asset:asset}).decimals;
    var ret = Nxt.call('transferAsset', {
      secretPhrase: this.secretPhrase,
      feeNQT: Nxt.util.convertToNQT('0.1'),
      deadline: '1440',      
      asset: asset,
      recipient: Nxt.util.convertRSAddress(recipientRS),
      quantityQNT: Nxt.util.convertToQNT(quantity, decimals)
    });
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return ret; }
    if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
    return ret;
  },
  placeAskOrder: function (asset, quantity, priceNXT, orderFeeQuantity) {
    var decimals = Nxt.call('getAsset', {asset:asset}).decimals;
    var quantityQNT = Nxt.util.convertToQNT(quantity, parseInt(decimals));
    var priceNQT = Nxt.util.calculatePricePerWholeQNT(Nxt.util.convertToNQT(priceNXT), parseInt(decimals));
    var argv = {
      secretPhrase: this.secretPhrase,
      feeNQT: Nxt.util.convertToNQT('0.1'),
      deadline: '1440',      
      asset: asset,
      priceNQT: priceNQT,
      quantityQNT: quantityQNT
    };
    if (typeof orderFeeQuantity != "undefined") {
      argv.orderFeeQNT = Nxt.util.convertToQNT(orderFeeQuantity, parseInt(decimals));
    }
    var ret = Nxt.call('placeAskOrder', argv);
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return ret; }
    if (!ret.order) ret.order = ret.transaction;
    if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
    return ret;
  },
  placeBidOrder: function (asset, quantity, priceNXT, orderFeeNXT) {
    var decimals = Nxt.call('getAsset', {asset:asset}).decimals;
    var quantityQNT = Nxt.util.convertToQNT(quantity, parseInt(decimals));
    var priceNQT = Nxt.util.calculatePricePerWholeQNT(Nxt.util.convertToNQT(priceNXT), parseInt(decimals));
    var argv = {
      secretPhrase: this.secretPhrase,
      feeNQT: Nxt.util.convertToNQT('0.1'),
      deadline: '1440',
      asset: asset,
      priceNQT: priceNQT,
      quantityQNT: quantityQNT
    };
    if (typeof orderFeeNXT != "undefined") {
      argv.orderFeeNQT = Nxt.util.convertToNQT(orderFeeNXT);
    }
    var ret = Nxt.call('placeBidOrder', argv);
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return ret; }
    if (!ret.order) ret.order = ret.transaction;    
    if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
    return ret;
  },
  cancelAskOrder: function (order) {
    var ret = Nxt.call('cancelAskOrder', {
      secretPhrase: this.secretPhrase,
      feeNQT: Nxt.util.convertToNQT('0.1'),
      deadline: '1440', 
      order: order
    });
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return ret; }
    if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
    return ret;    
  },
  cancelBidOrder: function (order) {
    var ret = Nxt.call('cancelBidOrder', {
      secretPhrase: this.secretPhrase,
      feeNQT: Nxt.util.convertToNQT('0.1'),
      deadline: '1440', 
      order: order
    });
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return ret; }
    if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
    return ret;
  },
  issueAsset: function (name, is_private, quantity, decimals) {
    var quantityQNT = Nxt.util.convertToQNT(quantity, parseInt(decimals));
    var argv = {
      secretPhrase: this.secretPhrase,
      feeNQT: Nxt.util.convertToNQT('1000'),
      deadline: '1440',      
      name: name,
      description: name + ' asset description',
      quantityQNT: quantityQNT,
      decimals: decimals,
      type: (is_private ? 1 : 0)
    };    
    var ret = Nxt.call('issueAsset', argv);
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return ret; }
    var id = ret.transaction;
    if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
    var asset = Nxt.call('getAsset', {asset: id}); 
    assert.assertEquals(asset.asset, id);
    asset.secretPhrase = this.secretPhrase;
    return Asset(asset);
  },
  publicKey: function () {
    return Nxt.call('getAccountPublicKey', { account: this.id_rs }).publicKey || null;
  },
  all: function () {
    return Nxt.call('getAccount', { account: this.id_rs });
  },
  setNamespacedAlias: function (key, value) {
    var ret = Nxt.call('setNamespacedAlias', {
      secretPhrase: this.secretPhrase,
      feeNQT: Nxt.util.convertToNQT('0.1'),
      deadline: '1440',
      aliasName: key,
      aliasURI: value
    });
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return ret; }    
    if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
    return ret;
  },
  getNamespacedAlias: function (key) {
    var ret = Nxt.call('getNamespacedAlias', {
      account: Nxt.util.convertRSAddress(this.id_rs),
      aliasName: key,
    });
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return null; }    
    return ret.aliasURI;
  },
  getNamespacedAliases: function (filter, firstIndex, lastIndex) {
    var arg = { 
      account: this.id_rs,
      firstIndex: firstIndex||0,
    };
    if (filter) { arg.filter = filter; }
    if (lastIndex) { arg.lastIndex = lastIndex; }
    var ret = Nxt.call('getNamespacedAliases', arg);
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return null; }    
    return ret;
  },
  setAccountIdentifier: function (identifier, signatory, signature, senderSecretPhrase) {
    var arg = {
      secretPhrase: senderSecretPhrase || this.secretPhrase,
      feeNQT: Nxt.util.convertToNQT('0.1'),
      deadline: '1440',      
      recipient: Nxt.util.convertRSAddress(this.id_rs),
      identifier: identifier
    };
    if (signatory) {
      arg.signatory = Nxt.util.convertRSAddress(signatory);
    }
    if (signature) {
      arg.signature = signature;
    }
    var ret = Nxt.call('setAccountIdentifier', arg);
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return null; }
    if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
    return ret;
  },
  getAccountIdentifiers: function () {
    var ret = Nxt.call('getAccountIdentifiers', { account: this.id_rs });
    if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return null; }    
    return ret.identifiers;
  }
};

var accounts = {
  forger: new Account(forgerSecretPhrase),
  a: new Account(secretPhrase1),
  b: new Account(secretPhrase2),
  c: new Account(secretPhrase3),
  d: new Account(secretPhrase4)    
};

function Asset(instance) {
  function translate(asset) {
    asset.orderFeePercentageHuman = Nxt.util.convertToQNTf(String(asset.orderFeePercentage), 6);
    asset.tradeFeePercentageHuman = Nxt.util.convertToQNTf(String(asset.tradeFeePercentage), 6);    
  }
  translate(instance);  
  extend(instance, {
    isPrivate: function () {
      return instance.type == 1;
    },
    quantity: function (quantityQNT) {
       return Nxt.util.convertToQNTf(String(quantityQNT), this.decimals);  
    },
    quantityQNT: function (quantity) {
       return Nxt.util.convertToQNT(String(quantity), this.decimals);    
    },
    getAccountAllowed: function (id) {
      var ret = Nxt.call('getPrivateAssetAccount', {asset: instance.asset, account: id});
      if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return null; }    
      return ret.allowed == "true";
    },
    setAccountAllowed: function (id, allowed) {
      var arg = {
        secretPhrase: this.secretPhrase,
        feeNQT: Nxt.util.convertToNQT('0.1'),
        deadline: '1440',      
        asset: instance.asset,
        recipient: id
      };
      allowed = (allowed==undefined||allowed);
      var requestType = allowed ? 'addPrivateAssetAccount' : 'removePrivateAssetAccount';
      var ret = Nxt.call(requestType, arg);
      if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return ret; }
      if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
      assert.assertEquals(allowed, instance.getAccountAllowed(id));
    },
    setFeeInternal: function (orderFeePercentage, tradeFeePercentage) {
      var arg = {
        secretPhrase: instance.secretPhrase,
        feeNQT: Nxt.util.convertToNQT('0.1'),
        deadline: '1440',      
        asset: instance.asset
      };
      if (orderFeePercentage!=null) { 
        arg.orderFeePercentage = orderFeePercentage;
      }
      if (tradeFeePercentage!=null) {
        arg.tradeFeePercentage = tradeFeePercentage;
      }
      var ret = Nxt.call('setPrivateAssetFee',arg);
      if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return ret; }
      if (Nxt.enableAutoForge) { Nxt.generateBlock(); }
      return ret;
    },
    setOrderFeePercentage: function (percentage) {
      this.reload();
      percentage = Nxt.util.convertToQNT(percentage, 6)||'0';
      var ret = this.setFeeInternal(percentage, instance.tradeFeePercentage);
      this.reload();
      return ret;
    },
    setTradeFeePercentage: function (percentage) {
      this.reload();
      percentage = Nxt.util.convertToQNT(percentage, 6)||'0';
      var ret = this.setFeeInternal(instance.orderFeePercentage, percentage);
      this.reload();
      return ret;    
    },
    reload: function () {
      var asset = Asset(Nxt.call('getAsset', {asset:instance.asset}));
      asset.secretPhrase = instance.secretPhrase;
      translate(asset);
      extend(this, asset);
    },
    getIssuerAccount: function () {
      return new Account(this.secretPhrase);
    },
    getAskOrders: function () {
      var ret = Nxt.call('getAskOrders', {asset:instance.asset});
      if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return []; }
      return ret.askOrders||[];
    },
    getBidOrders: function () {
      var ret = Nxt.call('getBidOrders', {asset:instance.asset});
      if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return []; }
      return ret.bidOrders||[];
    },
    getVirtualAskOrders: function (firstIndex, lastIndex) {
      var arg = {asset:instance.asset};
      if (typeof firstIndex == 'number') {
        arg.firstIndex = firstIndex;
        arg.lastIndex = lastIndex;
      }
      var ret = Nxt.call('getVirtualAskOrders', arg);
      if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return []; }
      return ret.askOrders||[];
    },
    getVirtualBidOrders: function (firstIndex, lastIndex) {
      var arg = {asset:instance.asset};
      if (typeof firstIndex == 'number') {
        arg.firstIndex = firstIndex;
        arg.lastIndex = lastIndex;
      }      
      var ret = Nxt.call('getVirtualBidOrders', arg);
      if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return []; }
      return ret.bidOrders||[];
    },
    getVirtualTrades: function (firstIndex, lastIndex) {
      var arg = {asset:instance.asset};
      if (typeof firstIndex == 'number') {
        arg.firstIndex = firstIndex;
        arg.lastIndex = lastIndex;
      }      
      var ret = Nxt.call('getVirtualTrades', arg);
      if (isError(ret)) { if (Nxt.verbose) { printError(ret); } return []; }
      return ret.trades||[];
    }
  });  
  return instance;
};

/**
 * Helper to test a matrix of balances
 * @param balances [ [ account, balance, assetBalance ] ]
 */
function assertBalance(asset, balances, name) {
  var account, balanceNXT, assetBalance, tmp;
  balances.forEach(function (b) {
    account = b[0], balanceNXT = b[1], assetBalance = b[2];
    if ((tmp = account.getBalanceNXT()) != balanceNXT) {
      console.log('['+name+'] Assert balanceNXT '+account.secretPhrase+' failed: expected ['+balanceNXT+'] but was ['+tmp+']');
    }
    assert.assertEquals(balanceNXT, tmp);
    if ((tmp = account.getAssetBalance(asset)) != assetBalance) {
      console.log('['+name+'] Assert assetBalance '+account.secretPhrase+' failed: expected ['+assetBalance+'] but was ['+tmp+']');
    }
    assert.assertEquals(assetBalance, tmp);
  });  
}