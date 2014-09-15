(function () {
'use strict';
var module = angular.module('fim.base');
module.controller('PaymentPluginCreateModalController', function(items, $modalInstance, 
  $scope, nxt, $timeout, $filter, i18n, alerts, $sce, db, plugins) {

  $scope.dialogName  = 'Send Payment';
  $scope.dialogTitle = $scope.dialogName;
  $scope.setTitle = function (text) {
    $timeout(function () {
      $scope.dialogTitle = $scope.dialogName + (text?(' | ' + text):'');
    });
  };  

  $scope.items = items;
  $scope.items.advanced = false;
  $scope.items.showPublicKey = items.showPublicKey || false;
  $scope.items.recipientDescriptionHTML = null;
  $scope.items.recipientAlertLevel = 'info'; // success, info, warning, danger
  $scope.items.amountNXT = items.amountNXT || '0';
  $scope.items.feeNXT = items.feeNXT || '0.1';
  $scope.items.deadline = items.deadline || '1440';
  $scope.items.valid = true;
  $scope.items.secretPhrase = items.secretPhrase || '';

  $scope.sendSuccess     = false;
  $scope.accounts        = [];
  $scope.selectedAccount = null;
  $scope.useSecretPhrase = false;
  $scope.senderRSCalc    = { id: 'FIM-?', balance: '?' };

  /* Load accounts from database */
  db.accounts.orderBy('name').toArray().then(
    function (accounts) {
      $timeout(function () { 
        $scope.accounts = accounts;
        $scope.selectedAccount = accounts[0];
        if ($scope.items.senderRS) {
          for (var i=0; i<$scope.accounts.length; i++) {
            if ($scope.accounts[i].id_rs == $scope.items.senderRS) {
              $scope.selectedAccount = accounts[i];
              break;
            }
          }
        }
      });
    }
  ).catch(alerts.catch("Could not load accounts"));  

  /* Register CRUD observer for accounts */
  db.accounts.addObserver($scope, 
    db.createObserver($scope, 'accounts', 'id_rs', {
      finally: function () {
        $scope.$applyAsync();
      }
    })
  );

  /* Special case when there are no accounts don't show the select list */
  db.accounts.count().then(
    function (count) {
      if (count == 0) {
        $timeout(function () {
          $scope.useSecretPhrase = true;
        });        
      }
    }
  );

  $scope.senderChanged = function () {
  };

  $scope.secretPhraseChanged = function () {
    $timeout(function () {
      $scope.senderRSCalc.id = nxt.crypto.getAccountId($scope.items.secretPhrase, true);
      $scope.senderRSCalc.balance = '?';
      nxt.getAccount({account:$scope.senderRSCalc.id}).then(
        function (account) {
          $timeout(function () {
            $scope.senderRSCalc.balance = nxt.util.convertToNXT(account.balanceNQT);
          });
        }
      );
    });
  };

  $scope.showAddAccount = function () {
    var account = {};
    plugins.get('accounts').add(account).then(
      function (items) {
        alerts.success("Successfully added account");
      }
    );
  };

  $scope.selectContact = function () {
    plugins.get('contacts').select().then(
      function (items) {
        $timeout(function () {
          $scope.items.recipientRS = items.id_rs;
          $scope.recipientChanged();
        });
      }
    );
  };  

  $scope.formatAccount = function (account) {
    return account.id_rs + ' - ' + account.name;
  };

  $scope.to_trusted = function(html_code) {
    return $sce.trustAsHtml(html_code);
  };

  $scope.close = function () {
    if ($scope.sendSuccess) {
      if ($scope.items.skipSaveContact) {
        $modalInstance.close($scope.items);
      }
      else {
        /* See if the recipient is already a contact */
        db.contacts.where('id_rs').equals($scope.items.recipientRS).toArray().then(
          function (contacts) {
            if (contacts.length == 0) {
              plugins.get('contacts').add({
                message: 'Do you want to add this contact?', 
                id_rs: $scope.items.recipientRS
              }).then(
                function () {
                  $modalInstance.close($scope.items);
                }
              );
            }
            else {
              $modalInstance.close($scope.items);
            }
          }
        );
      }
    }
    else {
      var args = {
        amountNQT:  nxt.util.convertToNQT($scope.items.amountNXT),
        feeNQT:     nxt.util.convertToNQT($scope.items.feeNXT),
        deadline:   $scope.items.deadline,
      };  

      /* Either provide the recipient publicKey or recipient id */      
      if ($scope.items.recipientPublicKey) {
        args.recipientPublicKey = $scope.items.recipientPublicKey;
        args.recipient = nxt.crypto.getAccountIdFromPublicKey(args.recipientPublicKey, false);
      }
      else {
        args.recipient = $scope.items.recipient;
      }

      /* */
      if ($scope.useSecretPhrase) {
        args.secretPhrase = $scope.items.secretPhrase;
        args.publicKey    = nxt.crypto.secretPhraseToPublicKey(args.secretPhrase);
      }
      else {
        args.sender = $scope.selectedAccount.id_rs;
      }

      nxt.sendMoney(args).then(
        function (data) {
          $timeout(function () {
            $scope.sendSuccess = true;
            angular.extend($scope.items, data);
          });          
        },
        alerts.catch('Could not send money')
      );
    }
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

  $scope.recipientChanged = function () {
    $scope.items.recipientPublicKey = null;
    $scope.items.recipientDescriptionHTML = null;
    $scope.items.showPublicKey = false;

    var account = $scope.items.recipientRS;
    var parts   = account.split(':');
    if (parts.length > 1) {
      $scope.items.recipientRS        = account = parts[0];
      $scope.items.recipientPublicKey = parts[1]; /* XXX this asumes no ':' character is ever in a public key */
      $scope.items.showPublicKey      = true;
    }
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
              $scope.items.recipientPublicKey = account.publicKey;
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