var NRS = (function(NRS, $, undefined) {
  NRS.fim = NRS.fim || {};
  var console = console || {log: function () {}};
  var deadline = null;  
  //var tooltip = 

  $(function () {
    //$('#account_start_forging_button').attr("title", tooltip);
    $('#account_start_forging_button').click(function (e) {
      e.preventDefault();
      if (NRS.downloadingBlockchain) {
        $.growl("The blockchain is busy downloading, you cannot forge during this time. Please try again when the blockchain is fully synced.", {
          "type": "danger"
        });
      } else if (NRS.state.isScanning) {
        $.growl("The blockchain is currently being rescanned, you cannot forge during this time. Please try again in a minute.", {
          "type": "danger"
        });
      } else if (!NRS.accountInfo.publicKey) {
        $.growl("You cannot forge because your account has no public key. Please make an outgoing transaction first.", {
          "type": "danger"
        });
      } else if (NRS.accountInfo.effectiveBalanceNXT == 0) {
        if (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom && NRS.lastBlockHeight <= NRS.accountInfo.currentLeasingHeightTo) {
          $.growl("Your effective balance is leased out, you cannot forge at the moment.", {
            "type": "danger"
          });
        } else {
          $.growl("Your effective balance is zero, you cannot forge.", {
            "type": "danger"
          });
        }
      } else {
        if (NRS.isForging) {
          $("#refresh_forging_modal").modal("show");
        } else {
          $("#start_forging_modal").modal("show"); 
        } 
      }      
    });
  });

  NRS.fim.setForgingDeadline = function (_deadline) {
    deadline = _deadline
    if (deadline != null) {
      deadline = Date.now() + (_deadline * 1000);
    }
  }
  
  function updateTicker() {
    setTimeout(function () {      
      try {
        if (deadline == null) {
          $('#account_forge_time').html("not forging");
        }
        else {
          var remaining = deadline - Date.now();
          if (remaining > 0) {
            var d = millisecondToDuration(remaining);
            $('#account_forge_time').html([pad(d.hours), pad(d.minutes), pad(d.seconds)].join(':'));
          }
          else {
            $('#account_forge_time').html('unknown');
          }
        }
      } catch (e) {
        console.log(e);
      }
      updateTicker();
    }, 990);  
  }
  
  /* XXX - Let the ticker run forever */
  updateTicker();
  
  function millisecondToDuration(mil) {
    var seconds = (mil / 1000) | 0;
    mil -= seconds * 1000;    
    var minutes = (seconds / 60) | 0;
    seconds -= minutes * 60;    
    var hours = (minutes / 60) | 0;
    minutes -= hours * 60;
    return { hours: hours, minutes: minutes, seconds: seconds };
  }
  
  function pad(x) {
    return (x < 10) ? '0'+x : x;
  }
  
  // function getDeadline(success, fail) {
    // try {
      // if (NRS.rememberPassword) {
        // var secretPhrase = sessionStorage.getItem("secret");
        // if (secretPhrase) {
          // NRS.sendRequest("startForging", {
            // "secretPhrase": secretPhrase
          // }, function(response) {
            // if ("deadline" in response) {
              // success(parseInt(String(response.deadline)));
            // } else {
              // fail(0);
            // }
          // });
        // }
      // }
    // } catch (e) {
      // fail(e);        
    // }
  // }  
              
  return NRS;
}(NRS || {}, jQuery));