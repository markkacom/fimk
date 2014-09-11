(function () {
'use strict';
var module = angular.module('fim.base');
module.run(function (nxt) {

  function convertNQT(amount, decimals) {
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

  function commaFormat(amount) {
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

  nxt.util = {
    convertToNXT: function (amountNQT) {
      return commaFormat(convertNQT(amountNQT, 8));
    },
    convertToNQT: convertToNQT
  };

});
})();