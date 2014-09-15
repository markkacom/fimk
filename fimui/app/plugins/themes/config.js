(function () {
'use strict';
var module = angular.module('fim.base');
module.run(function (modals, plugins) {
  var registry = {};
  
  /* Register as plugin */
  plugins.register({
    id: 'themes',
    label: 'Themes',
    registry: registry,
    switchTo: function (id) {
      $('#bootswatch.style').attr('href', registry[id].url);
    },
    register: function (id, label, url) {
      registry[id] = { label: label, url: url };
    },
    default: function () {
      return registry['default'];
    }
  });

  /* Register styles */
  angular.forEach(['amelia','cerulean','cosmo','custom','cyborg','darkly',
                   'default','flatly','journal','lumen','paper','readable',
                   'sandstone','simplex','slate','spacelab','superhero',
                   'united','yeti'], 
    function (id) {
      var capitalized = id.charAt(0).toUpperCase() + id.slice(1);
      registry[id] = { 
        id: id,
        label: capitalized, 
        url: ('plugins/themes/css/'+id+'.bootstrap.min.css')
      };
    }
  );

});

})();