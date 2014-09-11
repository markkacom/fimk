(function () {
'use strict';

var module = angular.module('fim.base');
module.factory('db', function ($log, $injector, alerts, $timeout) {

  var db = new Dexie('fimkrypto-db');
  db.version(1).stores({
    accounts:     "id_rs,&id,&name,balanceNXT,forgedBalanceNXT",
    transactions: "transaction,type,subtype,timestamp,recipientRS,senderRS,height,block",
    blocks:       "id,&height,generator_rs,fee_nxt,length"
  });

  /* Load models here to prevent cicrular dependency errors */
  $injector.get('Account').initialize(db);
  $injector.get('Transaction').initialize(db);
  $injector.get('Block').initialize(db);

  db.on('error', alerts.catch("Database error"));
  db.on('populate', function () {
    db.accounts.add({
      id_rs:  'FIM-9MAB-AXFN-XXL7-6BHU3',
      id:     '4677526180684090633',
      name:   'KryptoFin'
    });

    db.accounts.add({
      id_rs:  'FIM-R4X4-KMHT-RCXD-CLGFZ',
      id:     '12661257429910784930',
      name:   'Someone'
    });
  });
  db.on('changes', function (changes, partial) {
    var tables = {};
    changes.forEach(function (change) {
      var table = (tables[change.table]||(tables[change.table]={create:[],update:[],remove:[]}));
      switch (change.type) {
        case 1: { // CREATED
          table.create.push(change.obj);
          break;
        }
        case 2: { // UPDATED
          table.update.push(change.obj);
          break;
        }
        case 3: { // DELETED
          table.remove.push(change.oldObj);
          break;
        }
      };
    });
    $timeout(function () {
      angular.forEach(tables, function (table, key) {
        db[key].notifyObservers(function (observer) {
          observer.create(table.create);
          observer.update(table.update);
          observer.remove(table.remove);
          if (!partial) {
            observer.finally();
          }
        });
      });
    });
  });
  db.open();

  /**
   * The default observer is enough for most CRUD requirements.
   * It works on all tables where a table is a list of model objects.
   *
   * @param $scope    Current scope
   * @param name      Name of the array on scope that should mirror the models
   * @param indexName Name of the index on the model
   * @param observer  Standard optional observer, if a method is defined it is 
   *                  called after the crud operations.
   * */
  db.createObserver = function ($scope, name, indexName, observer) {
    return {
      create: function (models) {
        $scope[name] = $scope[name].concat(models);
        if (observer && observer.create) {
          observer.create(models);
        }
      },
      update: function (models) {
        angular.forEach(models, function (model) {
          var index = UTILS.findFirstPropIndex($scope[name], model, indexName);
          if (index > 0) {
            angular.extend($scope[name][index], model);
          }
        });
        if (observer && observer.update) {
          observer.update(models);
        }
      },
      remove: function (models) {
        angular.forEach(models, function (model) {
          var index = UTILS.findFirstPropIndex($scope[name], model, indexName);
          var old = $scope[name][index];
          if (old) {
            $scope[name].splice(index, 1);
          }
        });
        if (observer && observer.remove) {
          observer.remove(models);
        }        
      }, 
      finally: function () {
        if (observer && observer.finally) {
          observer.finally();
        } 
      }
    };
  };
  return db;
});

})();