'use strict'

app = angular.module 'fhirplaceGui', [
  'ngCookies',
  'ngAnimate',
  'ngSanitize',
  'ngRoute',
  'formstamp',
  "ui.codemirror"
], ($routeProvider) ->
    console.log('Im here')

    $routeProvider
      .when '/',
        templateUrl: '/views/welcome.html'
      .when '/conformance',
        templateUrl: '/views/conformance.html'
        controller: 'ConformanceCtrl'
      .when '/resources/:resourceType',
        templateUrl: '/views/resources/index.html'
        controller: 'ResourcesIndexCtrl'
      .when '/resources/:resourceType/new',
        templateUrl: '/views/resources/new.html'
        controller: 'ResourcesNewCtrl'
      .when '/resources/:resourceType/:resourceLogicalId',
        templateUrl: '/views/resources/show.html'
        controller: 'ResourceCtrl'
      .when '/resources/:resourceType/:resourceLogicalId/edit',
        templateUrl: '/views/resources/edit.html'
        controller: 'ResourcesShowCtrl'
      .when '/resources/:resourceType/:resourceLogicalId/validate',
        templateUrl: '/views/resources/validate.html'
        controller: 'ResourcesShowCtrl'
      .when '/resources/:resourceType/:resourceLogicalId/history',
        templateUrl: '/views/resources/history.html'
        controller: 'ResourcesHistoryCtrl'
      .otherwise
        redirectTo: '/'

app.filter 'uuid', ()->
  (id)->
    sid = id.substring(id.length - 6, id.length)
    "...#{sid}"

app.controller 'ConformanceCtrl', ($scope, $http) ->
    $scope.restRequestMethod = 'GET'
    $scope.restUri = '/metadata?_format=application/json'

    $http.get($scope.restUri)
      .success (data, status, headers, config) ->
        $scope.resources = data.rest[0].resources.sort (a, b) ->
          return -1 if a.type < b.type
          return 1 if a.type > b.type
          0
        data.rest = null
        $scope.conformance = data
        $scope.serverName = data.name
        $scope.resourceTypes = data.rest[0].resources
      .error (data, status, headers, config) ->
        console.log 'karamba'


app.controller 'ResourcesIndexCtrl', ($scope, $routeParams, $http) ->
  $scope.resourceType      = $routeParams.resourceType
  $scope.resourceTypeLabel = $scope.resourceType
  $scope.restRequestMethod = 'GET'
  $scope.restUri =
    "/#{$scope.resourceType}/_search?_format=application/json"

  $http.get($scope.restUri).success (data, status, headers, config) ->
    $scope.resources = data.map (resource) ->
      resource.prettyData = angular.toJson(angular.fromJson(resource.data), true)
      resource

app.controller 'ResourcesNewCtrl', ($scope, $routeParams, $http, $location) ->
  $scope.resourceType      = $routeParams.resourceType
  $scope.resourceTypeLabel = $routeParams.resourceType
  $scope.restRequestMethod = 'POST'
  $scope.restUri = "/#{$scope.resourceType}?_format=application/json"
  $scope.resource = {}

  headers = {'Content-Location': $scope.resourceContentLocation}
  $scope.save = ->
    $http(method: $scope.restRequestMethod, url: $scope.restUri, data: $scope.resource.content, headers: headers)
      .success (data, status, headers, config) ->
        console.log(data)
        $location.path("/resources/#{$scope.resourceType}")
      .error (data)->
        $scope.error = data


app.controller 'ResourceCtrl', ($scope, $routeParams, $http, $location) ->
  $scope.resourceType       = $routeParams.resourceType
  $scope.resourceLogicalId = $routeParams.resourceLogicalId
  $scope.resourceLabel     = $scope.resourceLogicalId
  $scope.restRequestMethod = 'GET'
  $scope.restUri =
    "/#{$scope.resourceType}/#{$scope.resourceLogicalId}?_format=application/json"

  loadResource = ()->
    $http.get($scope.restUri).success (data, status, headers, config) ->
      $scope.resource = {
        content: angular.toJson(angular.fromJson(data), true)
      }
      $scope.resourceContentLocation = headers('content-location')

  loadResource()

  $scope.save = ->
    $http(method: "PUT", url: $scope.restUri, data: $scope.resource.content, headers: {'Content-Location': $scope.resourceContentLocation})
      .success (data, status, headers, config) ->
        loadResource()
      .error (data)->
        $scope.error = data

  $scope.destroy = ->
    if window.confirm("Destroy #{$scope.resourceTypeLabel} #{$scope.resourceLabel}?")
      $http.delete($scope.restUri).success (data, status, headers, config) ->
        $location.path("/resources/#{$scope.resourceType}")

app.controller 'ResourcesHistoryCtrl', ($scope, $routeParams, $http) ->
  angular.extend($scope, $routeParams)
  $scope.restRequestMethod = 'GET'
  $scope.restUri =
    "/#{$scope.resourceType}/#{$scope.resourceLogicalId}/_history?_format=application/json"

  $http.get($scope.restUri).success (data, status, headers, config) ->
    $scope.resourceHistory  = angular.toJson(angular.fromJson(data), true)
    $scope.resourceVersions = data.entry


angular.module('fhirplaceGui')
  .controller 'ResourcesValidateCtrl', ($scope, $http) ->
    $scope.restRequestMethod = 'POST'
    $scope.restUri =
      "/#{$scope.resourceType}/_validate?_format=application/json"

    $scope.validate = ->
      if $scope.form.$valid
        $scope.resourceValidation = 'Validating ...'
        $http.post($scope.restUri, $scope.resource.prettyData)
          .success((data, status, headers, config) ->
            if data
              $scope.resourceValidation = angular.toJson(
                angular.fromJson(data),
                true
              )
            else
              $scope.resourceValidation = 'Everything is good'
          ).error (data, status, headers, config) ->
            $scope.resourceValidation = angular.toJson(
              angular.fromJson(data),
              true
            )
