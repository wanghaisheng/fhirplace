'use strict'

angular.module('fhirplaceGui', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ngRoute',
  'formstamp'
])
  .config ($routeProvider) ->

    $routeProvider
      .when '/',
        templateUrl: 'views/metadata/index.html'
        controller: 'MetadataIndexCtrl'
      .when '/resources/:resourceId',
        templateUrl: 'views/resources/index.html'
        controller: 'ResourcesIndexCtrl'
      .otherwise
        redirectTo: '/'
