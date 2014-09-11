(function () {
'use strict';

var module = angular.module('fim.base');

module.factory('nxt', function ($modal, $http, $q, modals, i18n) {

  var TEST_NET      = false;
  var PKANNOUNCEMENTBLOCKPASSED = TEST_NET ? true : false;
  var DGSBLOCKPASSED = TEST_NET ? true : false;

  var must_use_post = true;
  var canceller     = $q.defer();

  /* Dirty hack to detect if we are on https:wallet.fimk.fi */
  if (window.location.protocol.indexOf('https') == 0) {
    var host        = window.location.protocol + '//' + window.location.host;
  }
  else {
    /*var host        = 'http://127.0.0.1:7886'*/
    var host        = TEST_NET ? 'http://178.62.176.45:6886' : 'http://5.101.102.197:7886'; /* MAIN NET */
  }  

  var api = {
    getAccountTransactions: {
      args: {
        account:    {type: String, required: true}, 
        timestamp:  {type: Number},
        type:       {type: Number},
        subtype:    {type: Number}, 
        firstIndex: {type: Number},
        lastIndex:  {type: Number}
      },
      returns: {
        property: 'transactions'
      }
    },
    getBalance: {
      args: {
        account:    {type: String, required: true}
      }
    },
    getAccount: {
      args: {
        account:    {type: String, required: true} 
      }
    },
    sendMoney: {
      args: {
        sender:                       {type: String, argument: false},
        publicKey:                    {type: String}, // sender public key
        secretPhrase:                 {type: String},
        recipient:                    {type: String, required: true},
        recipientPublicKey:           {type: String},
        message:                      {type: String},
        messageIsText:                {type: Boolean},
        amountNQT:                    {type: String, required: true},
        feeNQT:                       {type: String, required: true},
        deadline:                     {type: String, required: true},
        encryptedMessageData:         {type: String},
        encryptedMessageNonce:        {type: String},
        messageToEncrypt:             {type: String},
        messageToEncryptIsText:       {type: Boolean},
        encryptToSelfMessageData:     {type: String},
        encryptToSelfMessageNonce:    {type: String},
        messageToEncryptToSelf:       {type: String},
        messageToEncryptToSelfIsText: {type: Boolean}
      }
    },
    getAccountPublicKey: {
      args: {
        account: {type: String, required: true}
      }
    },
    broadcastTransaction: {
      args: {
        transactionBytes: {type: String, required: true}
      }      
    }
  };

  function provideSecretPhrase(methodName, methodConfig, args) {
    var deferred = $q.defer();
    if (methodConfig.args.secretPhrase && !('secretPhrase' in args)) {
      modals.open('secretPhrase', {
        resolve: {
          items: function () {
            return angular.copy(args);
          }
        },
        close: function (items) {
          try {
            args.secretPhrase = items.secretPhrase;
            args.publicKey    = nxt.crypto.secretPhraseToPublicKey(args.secretPhrase);
            deferred.resolve();
          } catch (error) {
            deferred.reject(error);
          }
        },
        cancel: function (error) {
          deferred.reject(error);
        }
      });
    }
    else {
      deferred.resolve();
    }
    return deferred.promise;
  };

  function cancel_requests() {
    canceller.resolve();
    canceller = $q.defer();
  };

  function create_url(requestType) {
    return host + '/nxt?requestType=' + requestType + '&random=' + Math.random();
  };

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
  };

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
  };

  function sendRequest(methodName, methodConfig, args) {
    var deferred = $q.defer();    
    var promise  = methodConfig.secretPhrase ? 
          do_post(methodName, args) : 
          do_get(methodName, args);

    promise.then(
      function (data) {
        if (data.data.errorCode && !data.data.errorDescription) {
          data.data.errorDescription = (data.data.errorMessage ? data.data.errorMessage : "Unknown error occured.");
        }
        if (data.data.errorDescription) {
          deferred.reject(data.data);
        }
        else {
          deferred.resolve(data.data);
        }
      }, 
      function (error) {
        deferred.reject(error);
      }
    );
    return deferred.promise;
  };

  var nxt = {
    PKAnnouncementBlockPassed: PKANNOUNCEMENTBLOCKPASSED,
    dgsBlockPassed: DGSBLOCKPASSED
  };

  /* Declare all calls in api on the nxt nxt */
  angular.forEach(api, function (methodConfig, methodName) {

    /* Add method for API function */
    nxt[methodName] = function (args) {
      var deferred = $q.defer();
      provideSecretPhrase(methodName, methodConfig, args).then(
        function () {
          
          /* Test for missing arguments */
          for (var argName in methodConfig.args) {
            var argConfig = methodConfig.args[argName];
            if (argConfig.required && !(argName in args)) {
              deferred.reject("Missing required argument in "+methodName+" ["+argName+"]");
              return;
            }
          }

          /* Test argument type and unknown arguments */
          for (var argName in args) {
            var argValue = args[argName];
            if (!(argName in methodConfig.args)) {
              deferred.reject("Unexpected argument for "+methodName+" ["+argName+"]");
              return;
            }
            if (!(new Object(argValue) instanceof methodConfig.args[argName].type)) {
              deferred.reject("Argument for "+methodName+" ["+argName+"] of wrong type");
              return;
            }
          }
          
          /* NEVER send the secretPhrase to the server */
          var secretPhrase = args.secretPhrase;
          if ('secretPhrase' in args) {
            delete args.secretPhrase;
          }

          /* Make the call to the server */
          sendRequest(methodName, methodConfig, args).then(
            function (data) {

              /* The server prepared an unsigned transaction that we must sign and broadcast */
              if (secretPhrase && data.unsignedTransactionBytes) {
                var publicKey   = nxt.crypto.secretPhraseToPublicKey(secretPhrase);
                var signature   = nxt.crypto.signBytes(data.unsignedTransactionBytes, converters.stringToHexString(secretPhrase));

                /* Required by verifyAndSignTransactionBytes */
                args.publicKey  = publicKey;

                if (!nxt.crypto.verifyBytes(signature, data.unsignedTransactionBytes, publicKey)) {
                  deferred.reject(i18n.format('error_signature_verification_client'));
                  return;
                } 
                else {
                  var payload = verifyAndSignTransactionBytes(data.unsignedTransactionBytes, signature, methodName, args);
                  if (!payload) {
                    deferred.reject(i18n.format('error_signature_verification_server'));
                    return;
                  } 
                  else {
                    nxt.broadcastTransaction({transactionBytes: payload}).then(
                      function (data) {
                        deferred.resolve(data);
                      }
                    ).catch(alerts.catch("Could not broadcast transaction"));
                  }
                }
              }
              else if (methodConfig.returns) {
                if (methodConfig.returns.property) {
                  deferred.resolve(data[methodConfig.returns.property]);
                }
              }
              else {
                deferred.resolve(data);
              }
            }, 
            function (error) {
              deferred.reject(error);
            }
          );
        },
        function (error) {
          deferred.reject(error);
        }
      );
      return deferred.promise;
    };
  });

  function verifyAndSignTransactionBytes(transactionBytes, signature, requestType, data) {
    var transaction   = {};
    var byteArray     = converters.hexStringToByteArray(transactionBytes);
    transaction.type  = byteArray[0];

    if (nxt.dgsBlockPassed) {
      transaction.version = (byteArray[1] & 0xF0) >> 4;
      transaction.subtype = byteArray[1] & 0x0F;
    } else {
      transaction.subtype = byteArray[1];
    }

    transaction.timestamp = String(converters.byteArrayToSignedInt32(byteArray, 2));
    transaction.deadline  = String(converters.byteArrayToSignedShort(byteArray, 6));
    transaction.recipient = String(converters.byteArrayToBigInteger(byteArray, 8)); /* XXX - prevent transaction replay */
    transaction.publicKey = converters.byteArrayToHexString(byteArray.slice(16, 48));
    // transaction.recipient = String(converters.byteArrayToBigInteger(byteArray, 40)); /* XXX - prevent transaction replay */
    transaction.amountNQT = String(converters.byteArrayToBigInteger(byteArray, 48));
    transaction.feeNQT    = String(converters.byteArrayToBigInteger(byteArray, 56));

    var refHash = byteArray.slice(64, 96);
    transaction.referencedTransactionFullHash = converters.byteArrayToHexString(refHash);
    if (transaction.referencedTransactionFullHash == "0000000000000000000000000000000000000000000000000000000000000000") {
      transaction.referencedTransactionFullHash = "";
    }
    //transaction.referencedTransactionId = converters.byteArrayToBigInteger([refHash[7], refHash[6], refHash[5], refHash[4], refHash[3], refHash[2], refHash[1], refHash[0]], 0);

    transaction.flags = 0;

    if (transaction.version > 0) {
      transaction.flags         = converters.byteArrayToSignedInt32(byteArray, 160);
      transaction.ecBlockHeight = String(converters.byteArrayToSignedInt32(byteArray, 164));
      transaction.ecBlockId     = String(converters.byteArrayToBigInteger(byteArray, 168));
    }

    if (!("amountNQT" in data)) {
      data.amountNQT = "0";
    }

    if (!("recipient" in data)) {
      data.recipient    = "1739068987193023818";
      data.recipientRS  = "FIM-MRCC-2YLS-8M54-3CMAJ";
    }

    if (transaction.publicKey != data.publicKey) {
      return false;
    }

    if (transaction.deadline !== data.deadline) {
      return false;
    }

    if (transaction.recipient !== data.recipient) {
      if (data.recipient == "1739068987193023818" && transaction.recipient == "0") {
        //ok
      } else {
        return false;
      }
    }

    if (transaction.amountNQT !== data.amountNQT || transaction.feeNQT !== data.feeNQT) {
      return false;
    }

    if ("referencedTransactionFullHash" in data) {
      if (transaction.referencedTransactionFullHash !== data.referencedTransactionFullHash) {
        return false;
      }
    } else if (transaction.referencedTransactionFullHash !== "") {
      return false;
    }

    if (transaction.version > 0) {
      //has empty attachment, so no attachmentVersion byte...
      if (requestType == "sendMoney" || requestType == "sendMessage") {
        var pos = 176;
      } else {
        var pos = 177;
      }
    } else {
      var pos = 160;
    }

    switch (requestType) {
      case "sendMoney":
        if (transaction.type !== 0 || transaction.subtype !== 0) {
          return false;
        }
        break;
      case "sendMessage":
        if (transaction.type !== 1 || transaction.subtype !== 0) {
          return false;
        }

        if (!nxt.dgsBlockPassed) {
          var messageLength = String(converters.byteArrayToSignedInt32(byteArray, pos));
          pos += 4;
          var slice = byteArray.slice(pos, pos + messageLength);
          transaction.message = converters.byteArrayToHexString(slice);
          if (transaction.message !== data.message) {
            return false;
          }
        }
        break;
      case "setAlias":
        if (transaction.type !== 1 || transaction.subtype !== 1) {
          return false;
        }

        var aliasLength = parseInt(byteArray[pos], 10);
        pos++;
        transaction.aliasName = converters.byteArrayToString(byteArray, pos, aliasLength);
        pos += aliasLength;
        var uriLength = converters.byteArrayToSignedShort(byteArray, pos);
        pos += 2;
        transaction.aliasURI = converters.byteArrayToString(byteArray, pos, uriLength);
        pos += uriLength;
        if (transaction.aliasName !== data.aliasName || transaction.aliasURI !== data.aliasURI) {
          return false;
        }
        break;
      case "createPoll":
        if (transaction.type !== 1 || transaction.subtype !== 2) {
          return false;
        }

        var nameLength = converters.byteArrayToSignedShort(byteArray, pos);
        pos += 2;
        transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);
        pos += nameLength;
        var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);
        pos += 2;
        transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);
        pos += descriptionLength;
        var nr_options = byteArray[pos];
        pos++;
        for (var i = 0; i < nr_options; i++) {
          var optionLength = converters.byteArrayToSignedShort(byteArray, pos);
          pos += 2;
          transaction["option" + i] = converters.byteArrayToString(byteArray, pos, optionLength);
          pos += optionLength;
        }

        transaction.minNumberOfOptions = String(byteArray[pos]);
        pos++;
        transaction.maxNumberOfOptions = String(byteArray[pos]);
        pos++;
        transaction.optionsAreBinary = String(byteArray[pos]);
        pos++;

        if (transaction.name !== data.name || transaction.description !== data.description || 
            transaction.minNumberOfOptions !== data.minNumberOfOptions || 
            transaction.maxNumberOfOptions !== data.maxNumberOfOptions || 
            transaction.optionsAreBinary !== data.optionsAreBinary) {
          return false;
        }

        for (var i = 0; i < nr_options; i++) {
          if (transaction["option" + i] !== data["option" + i]) {
            return false;
          }
        }

        if (("option" + i) in data) {
          return false;
        }
        break;
      case "castVote":
        if (transaction.type !== 1 || transaction.subtype !== 3) {
          return false;
        }

        transaction.poll = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        var voteLength = byteArray[pos];
        pos++;
        transaction.votes = [];
        for (var i = 0; i < voteLength; i++) {
          transaction.votes.push(byteArray[pos]);
          pos++;
        }
        return false;

        break;
      case "hubAnnouncement":
        if (transaction.type !== 1 || transaction.subtype != 4) {
          return false;
        }

        var minFeePerByte = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        var numberOfUris = parseInt(byteArray[pos], 10);
        pos++;
        var uris = [];
        for (var i = 0; i < numberOfUris; i++) {
          var uriLength = parseInt(byteArray[pos], 10);
          pos++;
          uris[i] = converters.byteArrayToString(byteArray, pos, uriLength);
          pos += uriLength;
        }

        //do validation

        return false;

        break;
      case "setAccountInfo":
        if (transaction.type !== 1 || transaction.subtype != 5) {
          return false;
        }

        var nameLength = parseInt(byteArray[pos], 10);
        pos++;
        transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);
        pos += nameLength;
        var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);
        pos += 2;
        transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);
        pos += descriptionLength;
        if (transaction.name !== data.name || transaction.description !== data.description) {
          return false;
        }

        break;
      case "sellAlias":
        if (transaction.type !== 1 || transaction.subtype !== 6) {
          return false;
        }

        var aliasLength = parseInt(byteArray[pos], 10);
        pos++;
        transaction.alias = converters.byteArrayToString(byteArray, pos, aliasLength);
        pos += aliasLength;
        transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        if (transaction.alias !== data.aliasName || transaction.priceNQT !== data.priceNQT) {
          return false;
        }

        break;
      case "buyAlias":
        if (transaction.type !== 1 && transaction.subtype !== 7) {
          return false;
        }

        var aliasLength = parseInt(byteArray[pos], 10);
        pos++;
        transaction.alias = converters.byteArrayToString(byteArray, pos, aliasLength);
        pos += aliasLength;
        if (transaction.alias !== data.aliasName) {
          return false;
        }

        break;
      case "issueAsset":
        if (transaction.type !== 2 || transaction.subtype !== 0) {
          return false;
        }

        var nameLength = byteArray[pos];
        pos++;
        transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);
        pos += nameLength;
        var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);
        pos += 2;
        transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);
        pos += descriptionLength;
        transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        transaction.decimals = byteArray[pos];
        pos++;

        if (transaction.name !== data.name || transaction.description !== data.description || 
            transaction.quantityQNT !== data.quantityQNT || transaction.decimals !== data.decimals) {
          return false;
        }

        break;
      case "transferAsset":
        if (transaction.type !== 2 || transaction.subtype !== 1) {
          return false;
        }

        transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        if (!nxt.dgsBlockPassed) {
          var commentLength = converters.byteArrayToSignedShort(byteArray, pos);
          pos += 2;
          transaction.comment = converters.byteArrayToString(byteArray, pos, commentLength);
          if (transaction.comment !== data.comment) {
            return false;
          }
        }

        if (transaction.asset !== data.asset || transaction.quantityQNT !== data.quantityQNT) {
          return false;
        }
        break;
      case "placeAskOrder":
      case "placeBidOrder":
        if (transaction.type !== 2) {
          return false;
        } 
        else if (requestType == "placeAskOrder" && transaction.subtype !== 2) {
          return false;
        } 
        else if (requestType == "placeBidOrder" && transaction.subtype !== 3) {
          return false;
        }

        transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;

        if (transaction.asset !== data.asset || transaction.quantityQNT !== data.quantityQNT || 
            transaction.priceNQT !== data.priceNQT) {
          return false;
        }
        break;
      case "cancelAskOrder":
      case "cancelBidOrder":
        if (transaction.type !== 2) {
          return false;
        } 
        else if (requestType == "cancelAskOrder" && transaction.subtype !== 4) {
          return false;
        } 
        else if (requestType == "cancelBidOrder" && transaction.subtype !== 5) {
          return false;
        }

        transaction.order = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        if (transaction.order !== data.order) {
          return false;
        }

        break;
      case "dgsListing":
        if (transaction.type !== 3 && transaction.subtype != 0) {
          return false;
        }

        var nameLength = converters.byteArrayToSignedShort(byteArray, pos);
        pos += 2;
        transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);
        pos += nameLength;
        var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);
        pos += 2;
        transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);
        pos += descriptionLength;
        var tagsLength = converters.byteArrayToSignedShort(byteArray, pos);
        pos += 2;
        transaction.tags = converters.byteArrayToString(byteArray, pos, tagsLength);
        pos += tagsLength;
        transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));
        pos += 4;
        transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;

        if (transaction.name !== data.name || transaction.description !== data.description || 
            transaction.tags !== data.tags || transaction.quantity !== data.quantity || 
            transaction.priceNQT !== data.priceNQT) {
          return false;
        }

        break;
      case "dgsDelisting":
        if (transaction.type !== 3 && transaction.subtype !== 1) {
          return false;
        }

        transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        if (transaction.goods !== data.goods) {
          return false;
        }

        break;
      case "dgsPriceChange":
        if (transaction.type !== 3 && transaction.subtype !== 2) {
          return false;
        }

        transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        if (transaction.goods !== data.goods || transaction.priceNQT !== data.priceNQT) {
          return false;
        }

        break;
      case "dgsQuantityChange":
        if (transaction.type !== 3 && transaction.subtype !== 3) {
          return false;
        }

        transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        transaction.deltaQuantity = String(converters.byteArrayToSignedInt32(byteArray, pos));
        pos += 4;
        if (transaction.goods !== data.goods || transaction.deltaQuantity !== data.deltaQuantity) {
          return false;
        }

        break;
      case "dgsPurchase":
        if (transaction.type !== 3 && transaction.subtype !== 4) {
          return false;
        }

        transaction.goods = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));
        pos += 4;
        transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        transaction.deliveryDeadlineTimestamp = String(converters.byteArrayToSignedInt32(byteArray, pos));
        pos += 4;

        if (transaction.goods !== data.goods || transaction.quantity !== data.quantity || 
            transaction.priceNQT !== data.priceNQT || 
            transaction.deliveryDeadlineTimestamp !== data.deliveryDeadlineTimestamp) {
          return false;
        }

        break;
      case "dgsDelivery":
        if (transaction.type !== 3 && transaction.subtype !== 5) {
          return false;
        }

        transaction.purchase = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        var encryptedGoodsLength = converters.byteArrayToSignedShort(byteArray, pos);
        var goodsLength = converters.byteArrayToSignedInt32(byteArray, pos);
        transaction.goodsIsText = goodsLength < 0; // ugly hack??
        if (goodsLength < 0) {
          goodsLength &= 2147483647;
        }

        pos += 4;
        transaction.goodsData = converters.byteArrayToHexString(byteArray.slice(pos, pos + encryptedGoodsLength));
        pos += encryptedGoodsLength;
        transaction.goodsNonce = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
        pos += 32;
        transaction.discountNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        var goodsIsText = (transaction.goodsIsText ? "true" : "false");
        if (goodsIsText != data.goodsIsText) {
          return false;
        }

        if (transaction.purchase !== data.purchase || transaction.goodsData !== data.goodsData || 
            transaction.goodsNonce !== data.goodsNonce || transaction.discountNQT !== data.discountNQT) {
          return false;
        }

        break;
      case "dgsFeedback":
        if (transaction.type !== 3 && transaction.subtype !== 6) {
          return false;
        }

        transaction.purchase = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        if (transaction.purchase !== data.purchase) {
          return false;
        }

        break;
      case "dgsRefund":
        if (transaction.type !== 3 && transaction.subtype !== 7) {
          return false;
        }

        transaction.purchase = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        transaction.refundNQT = String(converters.byteArrayToBigInteger(byteArray, pos));
        pos += 8;
        if (transaction.purchase !== data.purchase || transaction.refundNQT !== data.refundNQT) {
          return false;
        }

        break;
      case "leaseBalance":
        if (transaction.type !== 4 && transaction.subtype !== 0) {
          return false;
        }

        transaction.period = String(converters.byteArrayToSignedShort(byteArray, pos));
        pos += 2;

        if (transaction.period !== data.period) {
          return false;
        }

        break;
      default:
        //invalid requestType..
        return false;
    }

    if (nxt.dgsBlockPassed) {
      var position = 1;

      //non-encrypted message
      if ((transaction.flags & position) != 0 || (requestType == "sendMessage" && data.message)) {
        var attachmentVersion = byteArray[pos];
        pos++;
        var messageLength = converters.byteArrayToSignedInt32(byteArray, pos);
        transaction.messageIsText = messageLength < 0; // ugly hack??

        if (messageLength < 0) {
          messageLength &= 2147483647;
        }

        pos += 4;
        if (transaction.messageIsText) {
          transaction.message = converters.byteArrayToString(byteArray, pos, messageLength);
        } 
        else {
          var slice = byteArray.slice(pos, pos + messageLength);
          transaction.message = converters.byteArrayToHexString(slice);
        }

        pos += messageLength;
        var messageIsText = (transaction.messageIsText ? "true" : "false");
        if (messageIsText != data.messageIsText) {
          return false;
        }

        if (transaction.message !== data.message) {
          return false;
        }
      } 
      else if (data.message) {
        return false;
      }

      position <<= 1;

      //encrypted note
      if ((transaction.flags & position) != 0) {
        var attachmentVersion = byteArray[pos];
        pos++;
        var encryptedMessageLength = converters.byteArrayToSignedInt32(byteArray, pos);
        transaction.messageToEncryptIsText = encryptedMessageLength < 0;

        if (encryptedMessageLength < 0) {
          encryptedMessageLength &= 2147483647; // http://en.wikipedia.org/wiki/2147483647
        }

        pos += 4;
        transaction.encryptedMessageData = converters.byteArrayToHexString(byteArray.slice(pos, pos + encryptedMessageLength));
        pos += encryptedMessageLength;
        transaction.encryptedMessageNonce = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
        pos += 32;
        var messageToEncryptIsText = (transaction.messageToEncryptIsText ? "true" : "false");
        if (messageToEncryptIsText != data.messageToEncryptIsText) {
          return false;
        }

        if (transaction.encryptedMessageData !== data.encryptedMessageData || transaction.encryptedMessageNonce !== data.encryptedMessageNonce) {
          return false;
        }
      } 
      else if (data.encryptedMessageData) {
        return false;
      }

      position <<= 1;

      if ((transaction.flags & position) != 0) {
        var attachmentVersion = byteArray[pos];
        pos++;
        var recipientPublicKey = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
        if (recipientPublicKey != data.recipientPublicKey) {
          return false;
        }
        pos += 32;
      } 
      else if (data.recipientPublicKey) {
        return false;
      }

      position <<= 1;

      if ((transaction.flags & position) != 0) {
        var attachmentVersion = byteArray[pos];
        pos++;
        var encryptedToSelfMessageLength = converters.byteArrayToSignedInt32(byteArray, pos);
        transaction.messageToEncryptToSelfIsText = encryptedToSelfMessageLength < 0;

        if (encryptedToSelfMessageLength < 0) {
          encryptedToSelfMessageLength &= 2147483647;
        }

        pos += 4;
        transaction.encryptToSelfMessageData = converters.byteArrayToHexString(byteArray.slice(pos, pos + encryptedToSelfMessageLength));
        pos += encryptedToSelfMessageLength;
        transaction.encryptToSelfMessageNonce = converters.byteArrayToHexString(byteArray.slice(pos, pos + 32));
        pos += 32;

        var messageToEncryptToSelfIsText = (transaction.messageToEncryptToSelfIsText ? "true" : "false");
        if (messageToEncryptToSelfIsText != data.messageToEncryptToSelfIsText) {
          return false;
        }

        if (transaction.encryptToSelfMessageData !== data.encryptToSelfMessageData || 
            transaction.encryptToSelfMessageNonce !== data.encryptToSelfMessageNonce) {
          return false;
        }
      } 
      else if (data.encryptToSelfMessageData) {
        return false;
      }
    }

    return transactionBytes.substr(0, 192) + signature + transactionBytes.substr(320);
  }

  return nxt;

});

})();