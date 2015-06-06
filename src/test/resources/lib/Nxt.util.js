(function () {
  var _hash = {
    init: SHA256_init,
    update: SHA256_write,
    getBytes: SHA256_finalize
  };
  
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
   * @param secretPhrase Ascii String
   * @returns hex-string 
   */
  function secretPhraseToPublicKey(secretPhrase) {
    var secretHex = converters.stringToHexString(secretPhrase);
    var secretPhraseBytes = converters.hexStringToByteArray(secretHex);
    var digest = simpleHash(secretPhraseBytes);
    return converters.byteArrayToHexString(curve25519.keygen(digest).p);
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
      var address = new NxtAddress('FIM');
      return address.set(accountId) ? address.toString() : '';
    } 
    return accountId;
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
  
  function convertNQT(amount, decimals) {
    if (typeof amount == 'undefined') {
      return '0';
    }
    var negative        = '',
        decimals        = decimals || 8,
        afterComma      = '',
        amount          = new BigInteger(String(amount)),
        factor          = String(Math.pow(10, decimals)),
        fractionalPart  = amount.mod(new BigInteger(factor)).toString();

    amount = amount.divide(new BigInteger(factor));
    if (amount.compareTo(BigInteger.ZERO) < 0) {
      amount = amount.abs();
      negative = '-';
    }
    if (fractionalPart && fractionalPart != "0") {
      afterComma = '.';
      for (var i=fractionalPart.length; i<decimals; i++) {
        afterComma += '0';
      }
      afterComma += fractionalPart.replace(/0+$/, "");
    }
    amount = amount.toString();
    return negative + amount + afterComma;
  }

  function convertToNQT(amountNXT) {
    if (typeof amountNXT == 'undefined') {
      return '0';
    }
    amountNXT   = String(amountNXT).replace(/,/g,'');
    var parts   = amountNXT.split(".");
    var amount  = parts[0];
    if (parts.length == 1) {
      var fraction = "00000000";
    } 
    else if (parts.length == 2) {
      if (parts[1].length <= 8) {
        var fraction = parts[1];
      } 
      else {
        var fraction = parts[1].substring(0, 8);
      }
    } 
    else {
      throw "Invalid input";
    }
    for (var i=fraction.length; i<8; i++) {
      fraction += "0";
    }
    var result = amount + "" + fraction;
    if (!/^\d+$/.test(result)) {
      throw "Invalid input.";
    }
    result = result.replace(/^0+/, "");
    if (result === "") {
      result = "0";
    }
    return result;
  }

  function convertToQNT(quantity, decimals) {
    if (typeof quantity == 'undefined') {
      return '0';
    }   
    quantity  = String(quantity);
    var parts = quantity.split(".");
    var qnt   = parts[0];
    if (parts.length == 1) {
      if (decimals) {
        for (var i = 0; i < decimals; i++) {
          qnt += "0";
        }
      }
    } 
    else if (parts.length == 2) {
      var fraction = parts[1];
      if (fraction.length > decimals) {
        throw "Fraction can only have " + decimals + " decimals max.";
      } 
      else if (fraction.length < decimals) {
        for (var i = fraction.length; i < decimals; i++) {
          fraction += "0";
        }
      }
      qnt += fraction;
    } 
    else {
      throw "Incorrect input";
    }
    //in case there's a comma or something else in there.. at this point there should only be numbers
    if (!/^\d+$/.test(qnt)) {
      throw "Invalid input. Only numbers and a dot are accepted.";
    }
    //remove leading zeroes
    return qnt.replace(/^0+/, "");
  }

  function convertToQNTf(quantity, decimals) {
    if (typeof quantity == 'undefined') {
      return '0';
    }     
    quantity = String(quantity);
    if (quantity.length < decimals) {
      for (var i = quantity.length; i < decimals; i++) {
        quantity = "0" + quantity;
      }
    }
    var afterComma = "";
    if (decimals) {
      afterComma = "." + quantity.substring(quantity.length - decimals);
      quantity = quantity.substring(0, quantity.length - decimals);
      if (!quantity) {
        quantity = "0";
      }
      afterComma = afterComma.replace(/0+$/, "");
      if (afterComma == ".") {
        afterComma = "";
      }
    }
    return quantity + afterComma;
  }

  function commaFormat(amount) {
    if (typeof amount == 'undefined') {
      return '0';
    }      
    var neg    = amount.indexOf('-') == 0 && (amount.shift());
    amount     = amount.split('.'); // input is result of convertNQT
    var parts  = amount[0].split("").reverse().join("").split(/(\d{3})/).reverse();
    var format = [];
    for(var i=0;i<parts.length;i++) { 
      if (parts[i]) {
        format.push(parts[i].split('').reverse().join(''));
      }
    }
    return (neg?'-':'')+format.join(',')+(amount.length==2?('.'+amount[1]):'');
  }

  function timestampToDate(timestamp) {
    var date = new Date(Date.UTC(2013, 10, 24, 12, 0, 0, 0) + timestamp * 1000);
    return date;
  }

  /**
   * Converts an UTC timestamp to a NXT epoch timestamp.
   * @param timestamp Number
   * @returns String
   */
  function convertToEpochTimestamp(timestamp) {
    return Math.floor((timestamp - Date.UTC(2013, 10, 24, 12, 0, 0, 0)) / 1000);
  }

  /**
   * Formats an asset quantity
   * @param quantityQNT String
   * @param decimals Number
   * @returns String
   */
  // function formatQuantity(quantityQNT, decimals) {}

  /**
   * Used for ask and bid orders to calculate the price per whole quant
   * @param priceNQT String
   * @param decimals Number
   * @returns String
   */
  function formatOrderPricePerWholeQNT(priceNQT, decimals) {
    var price = new BigInteger(String(priceNQT));
    return commaFormat(price.multiply(new BigInteger("" + Math.pow(10, decimals))).toString());
  }

  /**
   * Used to calc the total amount of NQT involved in this order
   * @param priceNQT String
   * @param quantityQNT String
   * @returns String
   */
  function calculateOrderTotalNQT(priceNQT, quantityQNT) {
    quantityQNT = new BigInteger(String(quantityQNT));
    priceNQT = new BigInteger(String(priceNQT));
    var orderTotal = quantityQNT.multiply(priceNQT);
    return orderTotal.toString();
  }

  function calculateOrderPricePerWholeQNT(price, decimals) {
    if (typeof price != "object") {
      price = new BigInteger(String(price));
    }
    return Nxt.util.convertToNXT(price.multiply(new BigInteger("" + Math.pow(10, decimals))));
  }  

  function calculatePricePerWholeQNT(price, decimals) {
    price = String(price);
    if (decimals) {
      var toRemove = price.slice(-decimals);

      if (!/^[0]+$/.test(toRemove)) {
        //return new Big(price).div(new Big(Math.pow(10, decimals))).round(8, 0);
        throw "Nxt.util.calculatePricePerWholeQNT() throws 'error_invalid_input' == "+toRemove;
      } 
      else {
        return price.slice(0, -decimals);
      }
    } else {
      return price;
    }
  }  

  function convertFromHex16(hex) {
    var j;
    var hexes = hex.match(/.{1,4}/g) || [];
    var back = "";
    for (j = 0; j < hexes.length; j++) {
      back += String.fromCharCode(parseInt(hexes[j], 16));
    }

    return back;
  }

  function convertFromHex8(hex) {
    var hex = hex.toString(); //force conversion
    var str = '';
    for (var i = 0; i < hex.length; i += 2)
      str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
    return str;
  }

  function convertRSAddress(text) {
    var address = new NxtAddress('FIM');
    if (address.set(text)) {
      return address.account_id();
    }
  }

  Nxt.util = {
    
    convertToNXT: function (amountNQT) {
      //return commaFormat(convertNQT(amountNQT, 8));
      return convertNQT(amountNQT, 8);
    },
    
    getAccountId: getAccountId,
    convertNQT: convertNQT,
    convertToNQT: convertToNQT,
    convertToQNTf: convertToQNTf,
    convertToQNT: convertToQNT,
    commaFormat: commaFormat,
    formatOrderPricePerWholeQNT: formatOrderPricePerWholeQNT,
    calculateOrderTotalNQT: calculateOrderTotalNQT,
    timestampToDate: timestampToDate,
    calculatePricePerWholeQNT: calculatePricePerWholeQNT,
    calculateOrderPricePerWholeQNT: calculateOrderPricePerWholeQNT,
    convertFromHex16:convertFromHex16,
    convertFromHex8:convertFromHex8,
    convertToEpochTimestamp: convertToEpochTimestamp,
    convertRSAddress: convertRSAddress,

    /**
     * @param nxtA String
     * @param nxtB String
     * @returns String
     * */
    safeAdd: function (nxtA, nxtB, format) {
      var a = new BigInteger(String(nxtA));
      var b = new BigInteger(String(nxtB));
      return a.add(b).toString();
    }
  };
})();