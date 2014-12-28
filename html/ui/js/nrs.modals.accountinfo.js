/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	
	/* XXX - Show account info editor only if enough funds */
	
	$("#account_info_modal").on("show.bs.modal", function(e) {
    if (NRS.accountInfo.balanceNQT && NRS.accountInfo.balanceNQT >= NRS.ONE_NXT) {
      $("#account_info_name").val(NRS.accountInfo.name);
      $("#account_info_description").val(NRS.accountInfo.description);
    }
    else {
      $.growl("You cannot store your account info because you have insufficient balance. " + 
              "Storing account info costs 1 FIM your balance is " + NRS.formatAmount(new BigInteger(NRS.accountInfo.balanceNQT)) + " FIM.", {
        "type": "danger"
      });
      return e.preventDefault();
    }
	});

	NRS.forms.setAccountInfoComplete = function(response, data) {
		var name = $.trim(String(data.name));
		if (name) {
			$("#account_name").html(name.escapeHTML()).removeAttr("data-i18n");
		} else {
			$("#account_name").html($.t("no_name_set")).attr("data-i18n", "no_name_set");
		}

		var description = $.trim(String(data.description));

		setTimeout(function() {
			NRS.accountInfo.description = description;
			NRS.accountInfo.name = name;
		}, 1000);
	}

	return NRS;
}(NRS || {}, jQuery));