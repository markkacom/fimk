(function () {
'use strict';
var module = angular.module('fim.base');
module.run(function (nxt) {

  var _sharedKeys = {};
  var _hash = {
    init: SHA256_init,
    update: SHA256_write,
    getBytes: SHA256_finalize
  };

  /**
   * @param secretPhrase Ascii String
   * @returns hex-string 
   */
  function secretPhraseToPublicKey(secretPhrase) {
    var secretPhraseHex   = converters.stringToHexString(secretPhrase);
    var secretPhraseBytes = converters.hexStringToByteArray(secretPhraseHex);
    var digest            = simpleHash(secretPhraseBytes);
    return converters.byteArrayToHexString(curve25519.keygen(digest).p);
  }

  /**
   * Asks the server an account public key
   * @param account String
   * @returns Promise that returns a hex-string 
   */
  function getAccountPublicKey(account) {
    var deferred = $q.defer();
    nxt.getAccountPublicKey({account:account}).then(
      function (data) {
        deferred.resolve(data.publicKey);
      }
    ).catch(alerts.catch('Could not get publickey'));
    return deferred.promise; 
  }

  /**
   * @param secretPhrase Ascii String
   * @returns hex-string 
   */
  function getPrivateKey(secretPhrase) {
    SHA256_init();
    SHA256_write(converters.stringToByteArray(secretPhrase));
    return converters.shortArrayToHexString(curve25519_clamp(converters.byteArrayToShortArray(SHA256_finalize())));
  }

  /**
   * @param secretPhrase Ascii String
   * @returns String 
   */
  function getAccountId(secretPhrase, RSFormat) {
    var publicKey = secretPhraseToPublicKey(secretPhrase);
    return getAccountIdFromPublicKey(publicKey, RSFormat);
  }

  /**
   * @param secretPhrase Hex String
   * @returns String 
   */  
  function getAccountIdFromPublicKey(publicKey, RSFormat) {
    _hash.init();
    _hash.update(converters.hexStringToByteArray(publicKey));

    var account   = _hash.getBytes();
    var slice     = (converters.hexStringToByteArray(converters.byteArrayToHexString(account))).slice(0, 8);
    var accountId = byteArrayToBigInteger(slice).toString();

    if (RSFormat) {
      var address = new NxtAddress();
      return address.set(accountId) ? address.toString() : '';
    } 
    return accountId;
  }

  /**
   * @param account       String
   * @param secretPhrase  String
   * @returns Promise ByteArray 
   */
  function getSharedKeyWithAccount(account, secretPhrase) {
    var deferred = $q.defer();
    if (account in _sharedKeys) {
      deferred.resolve(_sharedKeys[account]);
    }
    else {
      var privateKey  = converters.hexStringToByteArray(getPrivateKey(secretPhrase));
      getAccountPublicKey(account).then(
        function (publicKeyHex) {
          var publicKey   = converters.hexStringToByteArray(publicKeyHex);
          var sharedKey   = getSharedKey(privateKey, publicKey);

          var sharedKeys  = Object.keys(_sharedKeys);
          if (sharedKeys.length > 50) {
            delete _sharedKeys[sharedKeys[0]];
          }
          deferred.response((_sharedKeys[account] = sharedKey));
        }
      );
    }
    return deferred.promise();
  }

  /**
   * @param key1 ByteArray
   * @param key2 ByteArray
   * @returns ByteArray 
   */
  function getSharedKey(key1, key2) {
    return converters.shortArrayToByteArray(
              curve25519_(converters.byteArrayToShortArray(key1), 
                          converters.byteArrayToShortArray(key2), null));
  }

  /**
   * @param message       Hex String
   * @param secretPhrase  Hex String
   * @returns Hex String
   */
  function signBytes(message, secretPhrase) {
    var messageBytes      = converters.hexStringToByteArray(message);
    var secretPhraseBytes = converters.hexStringToByteArray(secretPhrase);

    var digest = simpleHash(secretPhraseBytes);
    var s = curve25519.keygen(digest).s;
    var m = simpleHash(messageBytes);

    _hash.init();
    _hash.update(m);
    _hash.update(s);
    var x = _hash.getBytes();

    var y = curve25519.keygen(x).p;

    _hash.init();
    _hash.update(m);
    _hash.update(y);
    var h = _hash.getBytes();

    var v = curve25519.sign(h, x, s);

    return converters.byteArrayToHexString(v.concat(h));
  }

  /**
   * @param signature     Hex String
   * @param message       Hex String
   * @param publicKey     Hex String
   * @returns Boolean
   */
  function verifyBytes(signature, message, publicKey) {
    var signatureBytes  = converters.hexStringToByteArray(signature);
    var messageBytes    = converters.hexStringToByteArray(message);
    var publicKeyBytes  = converters.hexStringToByteArray(publicKey);
    var v = signatureBytes.slice(0, 32);
    var h = signatureBytes.slice(32);
    var y = curve25519.verify(v, h, publicKeyBytes);

    var m = simpleHash(messageBytes);

    _hash.init();
    _hash.update(m);
    _hash.update(y);
    var h2 = _hash.getBytes();

    return areByteArraysEqual(h, h2);
  }

  /**
   * @param message ByteArray
   * @returns ByteArray
   */
  function simpleHash(message) {
    _hash.init();
    _hash.update(message);
    return _hash.getBytes();
  }

  /**
   * @param bytes1 ByteArray
   * @param bytes2 ByteArray   
   * @returns Boolean
   */
  function areByteArraysEqual(bytes1, bytes2) {
    if (bytes1.length !== bytes2.length) {
      return false;
    }
    for (var i = 0; i < bytes1.length; ++i) {
      if (bytes1[i] !== bytes2[i])
        return false;
    }
    return true;
  }

  /**
   * @param curve ShortArray
   * @returns ShortArray
   */
  function curve25519_clamp(curve) {
    curve[0] &= 0xFFF8;
    curve[15] &= 0x7FFF;
    curve[15] |= 0x4000;
    return curve;
  }

  /**
   * @param byteArray ByteArray
   * @param startIndex Int
   * @returns BigInteger
   */
  function byteArrayToBigInteger(byteArray, startIndex) {
    var value = new BigInteger("0", 10);
    var temp1, temp2;
    for (var i = byteArray.length - 1; i >= 0; i--) {
      temp1 = value.multiply(new BigInteger("256", 10));
      temp2 = temp1.add(new BigInteger(byteArray[i].toString(10), 10));
      value = temp2;
    }
    return value;
  }

  function aesEncrypt(plaintext, options) {
    if (!window.crypto && !window.msCrypto) {
      throw {
        "errorCode": -1,
        "message": $.t("error_encryption_browser_support")
      };
    }

    // CryptoJS likes WordArray parameters
    var text = converters.byteArrayToWordArray(plaintext);

    if (!options.sharedKey) {
      var sharedKey = getSharedKey(options.privateKey, options.publicKey);
    } else {
      var sharedKey = options.sharedKey.slice(0); //clone
    }

    for (var i = 0; i < 32; i++) {
      sharedKey[i] ^= options.nonce[i];
    }

    var key = CryptoJS.SHA256(converters.byteArrayToWordArray(sharedKey));

    var tmp = new Uint8Array(16);

    if (window.crypto) {
      window.crypto.getRandomValues(tmp);
    } else {
      window.msCrypto.getRandomValues(tmp);
    }

    var iv = converters.byteArrayToWordArray(tmp);
    var encrypted = CryptoJS.AES.encrypt(text, key, {
      iv: iv
    });

    var ivOut = converters.wordArrayToByteArray(encrypted.iv);

    var ciphertextOut = converters.wordArrayToByteArray(encrypted.ciphertext);

    return ivOut.concat(ciphertextOut);
  }

  function aesDecrypt(ivCiphertext, options) {
    if (ivCiphertext.length < 16 || ivCiphertext.length % 16 != 0) {
      throw {
        name: "invalid ciphertext"
      };
    }

    var iv = converters.byteArrayToWordArray(ivCiphertext.slice(0, 16));
    var ciphertext = converters.byteArrayToWordArray(ivCiphertext.slice(16));

    if (!options.sharedKey) {
      var sharedKey = getSharedKey(options.privateKey, options.publicKey);
    } else {
      var sharedKey = options.sharedKey.slice(0); //clone
    }

    for (var i = 0; i < 32; i++) {
      sharedKey[i] ^= options.nonce[i];
    }

    var key = CryptoJS.SHA256(converters.byteArrayToWordArray(sharedKey));

    var encrypted = CryptoJS.lib.CipherParams.create({
      ciphertext: ciphertext,
      iv: iv,
      key: key
    });

    var decrypted = CryptoJS.AES.decrypt(encrypted, key, {
      iv: iv
    });

    var plaintext = converters.wordArrayToByteArray(decrypted);

    return plaintext;
  }

  function encryptData(plaintext, options) {
    if (!window.crypto && !window.msCrypto) {
      throw {
        "errorCode": -1,
        "message": $.t("error_encryption_browser_support")
      };
    }

    if (!options.sharedKey) {
      options.sharedKey = getSharedKey(options.privateKey, options.publicKey);
    }

    var compressedPlaintext = pako.gzip(new Uint8Array(plaintext));

    options.nonce = new Uint8Array(32);

    if (window.crypto) {
      window.crypto.getRandomValues(options.nonce);
    } else {
      window.msCrypto.getRandomValues(options.nonce);
    }

    var data = aesEncrypt(compressedPlaintext, options);

    return {
      "nonce": options.nonce,
      "data": data
    };
  }

  function decryptData(data, options) {
    if (!options.sharedKey) {
      options.sharedKey = getSharedKey(options.privateKey, options.publicKey);
    }

    var compressedPlaintext = aesDecrypt(data, options);

    var binData = new Uint8Array(compressedPlaintext);

    var data = pako.inflate(binData);

    return converters.byteArrayToString(data);
  }

  /* Extends the nxt service */
  nxt.crypto = {
    secretPhraseToPublicKey: secretPhraseToPublicKey,
    getAccountId: getAccountId,
    getAccountIdFromPublicKey: getAccountIdFromPublicKey,
    signBytes: signBytes,
    verifyBytes: verifyBytes
  };
});
})();