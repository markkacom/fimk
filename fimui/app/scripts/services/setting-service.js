(function () {
'use strict';
var module = angular.module('fim.base');  
module.factory('settings', function($log, db, alerts, $timeout) {

  function resolve(id, value) {
    if (registry[id].resolve) {
      $timeout(function () {
        registry[id].resolve(value);
      });
    }    
  }

  db.settings.addObserver(null, {
    create: function (settings) {
      console.log('observer.create', settings);
      angular.forEach(settings, function (setting) { resolve(setting.id, setting.value); });
    },
    update: function (settings) {
      console.log('observer.update', settings);
      angular.forEach(settings, function (setting) { resolve(setting.id, setting.value); });
    }
  });

  var registry  = {};
  return {

    /**
     * All settings have a unique id which is made from several nested namespaces.
     * Namespaces are separated with dots to form unique ids.
     *
     * Settings have a type to protect against unsupported values. Users can use 
     * one of these values for type: String, Number, Boolean, Object.
     *
     * A setting of undefined is never supported, use null in that case and Object 
     * as type.
     *
     * @param settings Array of { id: String, value: Object, label: String, type: Object, resolve: Function }
     */
    initialize: function (settings) {
      angular.forEach(settings, function (setting) {
        /* Don't allow duplicate registrations of settings */
        if (setting.id in registry) {
          throw new Error('Duplicate initialization of setting '+setting.id);
        }

        /* Validate that the type and value are a match */
        if (!(new Object(setting.value) instanceof setting.type)) {
          throw new Error("Setting for "+setting.id+" of wrong type");
        }

        /* Store default settings in local registry */
        registry[setting.id] = setting;

        /* Initialization only happens the first time - so optimize for all other times */
        db.settings.where('id').equals(setting.id).first().then(
          function (stored_setting) {
            if (setting.resolve) {
              $timeout(function () {
                setting.resolve(stored_setting!==undefined ? stored_setting.value : setting.value);
              });
            }
          }
        ).catch(alerts.catch('Init settings'));
      });
    },

    /**
     * Update a setting.
     */
    update: function (id, value) {
      console.log('update.setting', { id: id, value: value });
      var setting = registry[id];
      if (!setting) {
        throw new Error("Unknown setting "+id);
      }

      /* Validate that the type and value are a match */
      if (!(new Object(value) instanceof setting.type)) {
        throw new Error("Setting for "+setting.id+" of wrong type");
      }

      setting.value = value;
      db.settings.put({
        id:    setting.id,
        value: setting.value,
        label: setting.label
      }).catch(alerts.catch('Update setting'));
    },

    /**
     * Accepts an iterator function that iterates over all settings that have a key
     * that (partially) matches the provided setting key.
     *
     * This allows to iterate over all settings for a certain namespace.
     * 
     * @param key String (partial) key
     */
    getAll: function (prefix, callback) {
      db.settings.where('id').startsWith(prefix).toArray().then(
        function (settings) {
          console.log('getAll.'+prefix, settings);
          callback(settings);
        }
      ).catch(alerts.catch('Settings'));
    }
  }
});
})();