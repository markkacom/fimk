(function () {
'use strict';
var module = angular.module('fim.base');  
module.factory('plugins', function($log) {
  var registry = {};
  return {
    
    /* Registers a plugin */
    register: function (options) {
      registry[options.id] = options;
    },  

    /* Call the callback for every plugin we find that matches the query */
    install: function (parentID, callback) {
      angular.forEach(registry, function (plugin, id) {
        if (plugin.extends == parentID) {
          callback(plugin);
        }
      });
    },

    /* Validates arguments based on a config object */
    validate: function (args, config) {
      /* Test for missing arguments */
      for (var argName in config) {
        var argConfig = config[argName];
        if (argConfig.required && !(argName in args)) {
          $log.error("Missing required argument "+argName);
          return false;
        }
      }
      /* Test argument type and unknown arguments */
      for (var argName in args) {
        var argValue = args[argName];
        if (!(argName in config)) {
          $log.error("Unexpected argument "+argName);
          return false;
        }
        if (!(new Object(argValue) instanceof config[argName].type)) {
          $log.error("Argument for "+argName+" of wrong type");
          return false;
        }
      }
      return true;
    }
  };
});
})();