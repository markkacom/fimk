(function () {
'use strict';
var module = angular.module('fim.base');

module.controller('exchangeController', function ($scope, $rootScope, $stateParams, $timeout, nxtAEExchange, modals) {

  $rootScope.exchange = nxtAEExchange;  
  $scope.selectedPair = null;
  $scope.selectedPairLabel = '';
  $scope.buyOrders = [];
  $scope.sellOrders = [];
  $scope.chartPoints = [];
  $scope.pairs = [];

  function getTradingPairs() {
    $rootScope.cancelAllRequests();
    $rootScope.exchange.getTradingPairs().then(
      function (pairs) {
        $scope.pairs = pairs;
        var pair = UTILS.findFirst($scope.pairs, function (pair) { 
          return pair.base.symbol == $stateParams.base && pair.quote.symbol == $stateParams.quote; 
        });
        pair = pair || $scope.pairs[0];
        $scope.selectedPair = pair;
        $scope.selectedPairLabel = pair.base.symbol + '/' + pair.quote.symbol;
      }, 
      function (error) {
        error.retry = getTradingPairs;
        $scope.alert.failed(error);    
      }
    );
  }

  getTradingPairs();

  $scope.$watch('selectedPair', function () {
    function getBuyOrders() {
      $rootScope.exchange.getBuyOrders($scope.selectedPair).then(
        function (buyOrders) {
          $scope.buyOrders = buyOrders;
        },
        function (error) {
          error.retry = getBuyOrders;
          $scope.alert.failed(error);   
        }
      );
    }

    function getSellOrders() {
      $rootScope.exchange.getSellOrders($scope.selectedPair).then(
        function (sellOrders) {
          $scope.sellOrders = sellOrders;
        },
        function (error) {
          error.retry = getSellOrders;
          $scope.alert.failed(error);   
        }
      );
    }

    function getTrades() {
      $rootScope.exchange.getTrades($scope.selectedPair).then(
        function (trades) {
          $scope.trades = trades;
        },
        function (error) {
          error.retry = getTrades;
          $scope.alert.failed(error);   
        }
      );
    }

    function getChartData() {
      $rootScope.exchange.getChartData($scope.selectedPair).then(
        function (points) {
          $scope.chartPoints = points;
        },
        function (error) {
          error.retry = getChartData;
          $scope.alert.failed(error);   
        }
      );        
    }

    if ($scope.selectedPair) {
      getBuyOrders();
      getSellOrders();
      getTrades();
      getChartData();
    }
  });

  function getBalance(asset) {
    var result = UTILS.findFirst($scope.balances, function (obj) {
      return obj.asset == asset.symbol;
    });
    return result ? result.balance : 0;
  };

  function getMaxAmount(price, balance) {

  }

  $scope.showBuyModal = function () {
    modals.open('buy', {
      resolve: {
        items: function () { 
          return {
            price: $scope.sellOrders[0].price,
            amount: 0,
            total: 0,
            fee: 0,
            sellOrders: $scope.sellOrders,
            selectedPair: $scope.selectedPair,
            balance: {
              quote: getBalance($scope.selectedPair.quote),
              base: getBalance($scope.selectedPair.base)
            }
          }
        }
      },
      close: function (items) {
        console.log(items);
        modals.open('warn', {
          resolve: {
            items: function () {
              return {
                title: 'Not enabled',
                message: 'This is a beta version, placing orders is not yet supported.'
              };
            }
          },
          close: function () {
            $scope.login();
          }
        });
      }
    });
  }

  $scope.showSellModal = function () {
    modals.open('sell', {
      resolve: {
        items: function () { 
          return {
            price: $scope.buyOrders[0].price,
            amount: 0,
            total: 0,
            fee: 0,
            buyOrders: $scope.buyOrders,
            selectedPair: $scope.selectedPair,
            balance: {
              quote: getBalance($scope.selectedPair.quote),
              base: getBalance($scope.selectedPair.base)
            }
          }
        }
      },
      close: function (items) {
        console.log(items);
        modals.open('warn', {
          resolve: {
            items: function () {
              return {
                title: 'Not enabled',
                message: 'This is a beta version, placing orders is not yet supported.'
              };
            }
          },
          close: function () {
            $scope.login();
          }
        });        
      }
    });
  }  

});

})();