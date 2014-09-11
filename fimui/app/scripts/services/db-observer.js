(function () {
'use strict';
var module = angular.module('fim.base');
module.factory('dbObserver', function ($log, $timeout) {

  /* Dexie observer */
  var DBObserver = function (table, $scope) {
    
    /* Manualy turn IndexedDB objects into classes - Source: Dexie.js */
    var constructor   = table.schema.mappedClass;
    var use_proto     = (function () { function F() { }; var a = new F(); try { a.__proto__ = Object.prototype; return !(a instanceof F) } catch (e) { return false; } })();
    var makeInherited = use_proto ?
      function makeInherited(obj) {
        if (!obj) return obj; // No valid object. (Value is null). Return as is.
        // The JS engine supports __proto__. Just change that pointer on the existing object. A little more efficient way.
        obj.__proto__ = constructor.prototype;
        return obj;
      } : function makeInherited(obj) {
        if (!obj) return obj; // No valid object. (Value is null). Return as is.
        // __proto__ not supported - do it by the standard: return a new object and clone the members from the old one.
        var res = Object.create(constructor.prototype);
        for (var m in obj) if (obj.hasOwnProperty(m)) res[m] = obj[m];
        return res;
      };

    var self    = this;
    var $create = { objs: [], timeout: null };
    var $update = { objs: [], timeout: null };
    var $remove = { objs: [], timeout: null };

    function createTimeout(options) {
      return window.setTimeout(function () {
        options.timeout = null;
        var objs        = angular.copy(options.objs);
        options.objs    = [];
        $timeout(function () {
          angular.forEach(objs, function (obj) {
            constructor ? options.fn(makeInherited(obj)) : options.fn(obj);
          });
          if (self.$finally) self.$finally();
        });
      }, 0);
    }

    function create_hook(primKey, obj, trans) {
      console.log('hook-creating', primKey);
      $create.objs.push(obj);
      if ($create.timeout === null) {
        $create.timeout = createTimeout($create);
      }
    }

    function update_hook(mod, primKey, obj, trans) {
      console.log('hook-updating', obj);      
      $update.objs.push(obj);
      if ($update.timeout === null) {
        $update.timeout = createTimeout($update);
      }
    }

    function remove_hook(primKey, obj, trans) {
      console.log('hook-delete', primKey);
      $remove.objs.push(obj);
      if ($remove.timeout === null) {
        $remove.timeout = createTimeout($remove);
      }
    }

    /* Public methods */
    this.create = function (callback) {
      $create.fn = callback;
      table.hook('creating').subscribe(create_hook);
      return this;
    };

    this.update = function (callback) {
      $update.fn = callback;
      table.hook('updating').subscribe(update_hook);
      return this;
    };

    this.remove = function (callback) {
      $remove.fn = callback;
      table.hook('deleting').subscribe(remove_hook);
      return this;
    };

    this["finally"] = function (callback) {
      this.$finally = callback;
      return this;
    };

    this["catch"] = function (callback) {
      this.$catch = callback;
      return this;
    };

    /* Remove the db observer callbacks when the $scope is destroyed */
    $scope.$on("$destroy", function(){
      console.log('$scope.destroy');
      if ($create.fn) {
        $create.fn = null;
        table.hook('creating').unsubscribe(create_hook);
      }
      if ($update.fn) {
        $update.fn = null;
        table.hook('updating').unsubscribe(update_hook);
      }
      if ($remove.fn) {
        $remove.fn = null;
        table.hook('deleting').unsubscribe(remove_hook);
      }
    });
  };

  return {
    observe: function (table, $scope) {
      return new DBObserver(table, $scope);
    }
  };
});

})();