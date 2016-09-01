(function(app) {
  app.AppComponent =
    ng.core.Component({
      selector: 'orcid-app',
      template: '<h1>Orcid Angular 2 setup ready</h1>'
    })
    .Class({
      constructor: function() {}
    });
})(window.app || (window.app = {}));
