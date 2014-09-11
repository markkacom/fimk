(function(){

  function TableObserverAddon(db) {
    
    db.Table.prototype._observers  = [];
    
    db.Table.prototype.addObserver = function (observer, $scope) {
      var index = this._observers.indexOf(observer);
      if (index === -1) {
        this._observers.push(observer);  
      }
      /* Auto remove observer when $scope was provided */
      if ($scope) {
        var self = this;
        $scope.$on("$destroy", function() { self.removeObserver(observer); });
      }
    };

    db.Table.prototype.removeObserver = function (observer) {
      var index = this._observers.indexOf(observer);
      if (index !== -1) {
        this._observers.splice(index, 1);
      }
    };

    db.Table.prototype.notifyObservers = function (callback) {
      angular.forEach(this._observers, function (observer) {
        callback(observer);
      });
    };
  }

  Dexie.addons.push(TableObserverAddon);
})();