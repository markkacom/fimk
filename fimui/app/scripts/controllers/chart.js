(function () {
'use strict';
var module = angular.module('dgex.base');

var primary_color = null;
function getBootstrapPrimaryColor() {
  if (primary_color == null) {
    var p = $("<a class='btn btn-primary'></a>").hide().appendTo("body");
    primary_color = p.css("background-color");
    p.remove();
  }
  return primary_color;
}

/*
 * Data example: { 
 *    time: 111, 
 *    open: 1,
 *    high: 10,
 *    low: 5,
 *    close: 20,
 *    average: 15,
 *    volume: 10000
 * }    
 */
module.controller('chartController', function ($scope) {
  $scope.$watch('chartPoints', function () {
    var points = $scope.chartPoints;
    var pair = $scope.selectedPair;

    if (points.length == 0) {
      $('#chart-container').highcharts('StockChart', { series: [], credits: { enabled: false } });
      return;
    }

    var data = { average: [], volume: [], ohlc: [] };
    var showCandleStick = 'high' in points[0];
    var showAverage = 'average' in points[0];
    var showVolume = 'volume' in points[0];

    for (var i=0; i<points.length; i++) {
      if (showCandleStick) {
        data.ohlc.push([
          points[i].time,   // the date
          points[i].open,   // open
          points[i].high,   // high
          points[i].low,    // low
          points[i].close,  // close
        ]);
      }
      if (showAverage) {
        data.average.push([points[i].time, points[i].average]);
      }
      if (showVolume) {
        data.volume.push([points[i].time, points[i].volume]);
      }
    }

    // set the allowed units for data grouping
    var groupingUnits = [[
      'week',                         // unit name
      [1]                             // allowed multiples
    ], [
      'month',
      [1, 2, 3, 4, 6]
    ]];
    
    var chartConfig = {
      // title : { 
      //   text : exchange.current.label + ' - ' + pair.base.symbol + ' / ' + pair.quote.symbol 
      // },
      credits: { enabled: false },
      series: [],
      yAxis: [],
      rangeSelector : {
        inputEnabled: $('#chart-container').width() > 480,
        selected : 2,
        buttons: [
          { type: 'hour', count: 6, text: '6h' }, 
          { type: 'day', count: 1, text: '1d' }, 
          { type: 'week', count: 1, text: '1w' }, 
          { type: 'month', count: 1, text: '1m' }, 
          { type: 'month', count: 3, text: '3m' }, 
          { type: 'year', count: 1, text: '1y' },
          { type: 'all', text: 'All' }
        ]
      },
      chart: {
        renderTo: 'chart-container'
      }
    };

    /* Candle stick or linechart */
    if (showAverage) {
      chartConfig.series.push({
        name : 'Price',
        color: getBootstrapPrimaryColor(),
        data : data.average,
        marker : {
          enabled : true,
          radius : 3
        },
        shadow : true,
        tooltip : {
          valueDecimals: pair.base.decimals,
          valueSuffix: ' ' + pair.base.symbol                
        }
      });
      chartConfig.yAxis.push({
        labels: {
          align: 'right',
          x: -3
        },
        title: {
          text: 'Price'
        },
        height: (showVolume ? '60%' : '100%'),
        lineWidth: 2
      });
    }
    else if (showCandleStick) {
      chartConfig.series.push({
        type: 'candlestick',
        name : 'OHLC',
        data : data.ohlc,
        marker : {
          enabled : true,
          radius : 3
        },
        shadow : true,
        tooltip : {
          valueDecimals : pair.base.decimals
        },
        //dataGrouping: { units: groupingUnits }
      });
      chartConfig.yAxis.push({
        labels: {
          align: 'right',
          x: -3
        },
        title: {
          text: 'OHLC'
        },
        height: (showVolume ? '60%' : '100%'),
        lineWidth: 2
      });
    }

    if (showVolume) {
      chartConfig.series.push({
        type: 'column',
        color: getBootstrapPrimaryColor(),
        name: 'Volume',
        data: data.volume,
        yAxis: 1,
        //dataGrouping: { units: groupingUnits }
      });
      chartConfig.yAxis.push({
        labels: {
          align: 'right',
          x: -3
        },
        title: {
          text: 'Volume'
        },
        top: '65%',
        height: '35%',
        offset: 0,
        lineWidth: 2
      });
    }
    $('#chart-container').highcharts('StockChart', chartConfig);    
  });
});
})();