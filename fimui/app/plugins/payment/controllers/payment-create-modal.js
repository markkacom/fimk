(function () {
'use strict';
var module = angular.module('fim.base');
module.controller('PaymentPluginCreateModalController', function(items, $modalInstance, 
  $scope, nxt, $timeout, $filter, i18n, alerts, $sce) {

  $scope.items = items;
  $scope.items.advanced = false;
  $scope.items.showPublicKey = false;
  $scope.items.recipientDescriptionHTML = null;
  $scope.items.recipientAlertLevel = 'info'; // success, info, warning, danger
  $scope.items.amountNXT = $scope.items.amountNXT || '0';
  $scope.items.feeNXT = $scope.items.feeNXT || '0.1';
  $scope.items.deadline = $scope.items.deadline || '1440';
  $scope.items.valid = true;

  $scope.to_trusted = function(html_code) {
    return $sce.trustAsHtml(html_code);
  };

  $scope.close = function () {
    nxt.sendMoney({
      amountNQT:  nxt.util.convertToNQT($scope.items.amountNXT),
      feeNQT:     nxt.util.convertToNQT($scope.items.feeNXT),
      deadline:   $scope.items.deadline,
      recipient:  $scope.items.recipient,
      sender:     $scope.items.senderRS
    }).then(
      function (data) {
        alerts.success('Payment send successfully');
        $modalInstance.close($scope.items);
      },
      alerts.catch('Could not send money')
    );
  };

  $scope.dismiss = function () {
    $modalInstance.dismiss();
  };

  function setDescription(level, html) {
    console.log('setDescription.level', level);
    console.log('setDescription.html', html);
    $timeout(function () {
      $scope.items.recipientAlertLevel = level;
      $scope.items.recipientDescriptionHTML = html;
    });
  }

  $scope.correctAddressMistake = function (element) {
    console.log('correctAddressMistake', element);
    $('form[name=paymentCreateForm] input[name=recipient]').val(element.getAttribute('data-address')).change();
  };

  $scope.recipientChanged = function (element) {
    $scope.items.recipientPublickey = null;
    $scope.items.recipientDescriptionHTML = null;
    $scope.items.showPublicKey = false;

    var account = $scope.items.recipientRS;
    if (/^(FIM\-)?[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+/i.test(account)) {
      var address = new NxtAddress();
      if (address.set(account)) {
        nxt.getAccount({account: account}).then(
          function (account) {
            console.log('account', account);
            if (!account.publicKey) {
              $scope.items.showPublicKey = true;
              setDescription('warning', i18n.format('recipient_no_public_key', {__nxt__: nxt.util.convertToNXT(account.unconfirmedBalanceNQT) }));
            }
            else {
              $scope.items.recipientPublickey = account.publicKey;
              $scope.items.recipient = account.account;
              setDescription('info', i18n.format('recipient_info', {__nxt__: nxt.util.convertToNXT(account.unconfirmedBalanceNQT) })); 
            }
          },
          function (error) {
            if (error.errorCode == 4) {
              setDescription('danger', i18n.format('recipient_malformed'));
            }
            else if (error.errorCode == 5) {
              setDescription('warning', i18n.format('recipient_unknown'));
            }
            else {
              setDescription('danger', i18n.format('recipient_problem', {__problem__: String(error.errorDescription).escapeHTML()}));
            }
          }
        );
      }
      else {
        if (address.guess.length == 1) {
          setDescription('warning', i18n.format('recipient_malformed_suggestion', {
            __recipient__: '<span class="malformed_address" data-address="' + String(address.guess[0]).escapeHTML() + '" ' +
                           'onclick="angular.element(this).scope().correctAddressMistake(this)">' + 
                              address.format_guess(address.guess[0], account) + '</span>'
          }));
        }
        else if (address.guess.length > 1) {
          var html = '<ul>';
          for (var i = 0; i < address.guess.length; i++) {
            html += '<li>';
            html += '<span class="malformed_address" data-address="' + String(address.guess[i]).escapeHTML() + '" ' +
                           'onclick="angular.element(this).scope().correctAddressMistake(this)">' + 
                              address.format_guess(address.guess[i], account) + '</span>';
            html += '</li>';
          }
          html += '</ul>';
          setDescription('warning', i18n.format('recipient_malformed_suggestion_plural', { __multiple__: html }));
        }
        else {
          setDescription('danger', i18n.format('recipient_malformed'));
        }
      }
    }
    else if (account.trim().length > 0) {
      setDescription('danger', i18n.format('recipient_malformed'));
    }
  };

});
})();