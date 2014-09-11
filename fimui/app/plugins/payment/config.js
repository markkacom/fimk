(function () {
'use strict';
var module = angular.module('fim.base');
module.run(function (modals, plugins) {

  var config = {
    senderRS:     { type: String },
    recipientRS:  { type: String },
    amountNXT:    { type: String },
    feeNXT:       { type: String },
    message:      { type: String },
    secretPhrase: { type: String },
  };
  
  /* Register as plugin */
  plugins.register({
    id: 'payment',
    extends: 'accounts',
    label: 'Payment',
    create: function (args) {
      if (plugins.validate(args, config)) {
        modals.open('paymentCreate', {
          resolve: {
            items: function () {
              return angular.copy(args);
            }
          }
        });
      }
    }
  });

  /* Register modal dialogs */
  modals.register('paymentCreate', { 
    templateUrl: 'plugins/payment/partials/payment-create-modal.html', 
    controller: 'PaymentPluginCreateModalController' 
  });
});

})();