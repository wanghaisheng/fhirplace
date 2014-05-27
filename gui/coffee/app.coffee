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

app.run ($rootScope)->
  $rootScope._menu = [{url: '/conformance', label: 'Conformance'}]
  $rootScope.menu = [{url: '/conformance', label: 'Conformance'}]

cropUuid = (id)->
  sid = id.substring(id.length - 6, id.length)
  "...#{sid}"

app.filter 'uuid', ()->
  cropUuid

app.controller 'ConformanceCtrl', ($rootScope, $scope, $http) ->
    $scope.restRequestMethod = 'GET'
    $scope.restUri = '/metadata?_format=application/json'
    $rootScope.menu = $scope._menu.slice(0)

    $http.get($scope.restUri)
      .success (data, status, headers, config) ->
        $scope.resources = (data.rest[0] || []).resources.sort (a, b) ->
          return -1 if a.type < b.type
          return 1 if a.type > b.type
          0
        data.rest = null
        $scope.conformance = data
        $scope.serverName = data.name
      .error (data, status, headers, config) ->
        console.log 'karamba'


app.controller 'ResourcesIndexCtrl', ($rootScope, $scope, $routeParams, $http) ->
  angular.extend($scope, $routeParams)
  $scope.restRequestMethod = 'GET'
  rt = $scope.resourceType

  $scope.restUri = "/#{rt}/_search?_format=application/json"

  $rootScope.menu = $rootScope._menu.slice(0)
  $rootScope.menu.push(url: "/resources/#{rt}", label: rt)
  $rootScope.menu.push(url: "/resources/#{rt}/new", label: "New", icon: "fa-plus")

  $http.get($scope.restUri).success (data, status, headers, config) ->
    $scope.resources = data.map (resource) ->
      resource.prettyData = angular.toJson(angular.fromJson(resource.data), true)
      resource

app.controller 'ResourcesNewCtrl', ($rootScope, $scope, $routeParams, $http, $location) ->
  angular.extend($scope, $routeParams)

  rt = $scope.resourceType

  $rootScope.menu = $rootScope._menu.slice(0)
  $rootScope.menu.push(url: "/resources/#{rt}", label: rt)
  $rootScope.menu.push(url: "/resources/#{rt}/new", label: "New", icon: "fa-plus")

  $scope.restRequestMethod = 'POST'
  $scope.restUri = "/#{$scope.resourceType}?_format=application/json"
  $scope.resource = {}

  $scope.progress || = {}

  headers = {'Content-Location': $scope.resourceContentLocation}
  $scope.save = ->
    $scope.progress['save'] = 'Creating...'
    $http(method: $scope.restRequestMethod, url: $scope.restUri, data: $scope.resource.content, headers: headers)
      .success (data, status, headers, config) ->
        delete $scope.progress['save']
        $location.path("/resources/#{$scope.resourceType}")
      .error (data)->
        delete $scope.progress['save']
        $scope.error = data


app.controller 'ResourceCtrl', ($rootScope, $scope, $routeParams, $http, $location) ->
  angular.extend($scope, $routeParams)
  $scope.restRequestMethod = 'GET'
  $scope.restUri = "/#{$scope.resourceType}/#{$scope.resourceLogicalId}?_format=application/json"

  rt = $scope.resourceType
  id = $scope.resourceLogicalId
  $rootScope.menu = $rootScope._menu.slice(0)
  $rootScope.menu.push(url: "/resources/#{rt}", label: rt)
  $rootScope.menu.push(url: "/resources/#{rt}/#{id}", label: cropUuid(id))
  $rootScope.menu.push(url: "/resources/#{rt}/#{id}/history", label: 'History', icon: 'fa-history')

  loadResource = ()->
    $http.get($scope.restUri).success (data, status, headers, config) ->
      $scope.resource = {
        content: angular.toJson(angular.fromJson(data), true)
      }
      $scope.resourceContentLocation = headers('content-location')

  loadResource()
  $scope.progress || = {}

  $scope.save = ->
    $scope.progress['save'] = 'Updating...'
    $http(method: "PUT", url: $scope.restUri, data: $scope.resource.content, headers: {'Content-Location': $scope.resourceContentLocation})
      .success (data, status, headers, config) ->
        delete $scope.progress['save']
        loadResource()
      .error (data)->
        delete $scope.progress['save']
        $scope.error = data

  $scope.destroy = ->
    $scope.progress['delete'] = 'Deleting...'
    if window.confirm("Destroy #{$scope.resourceTypeLabel} #{$scope.resourceLabel}?")
      $http.delete($scope.restUri).success (data, status, headers, config) ->
        $location.path("/resources/#{$scope.resourceType}")

app.controller 'ResourcesHistoryCtrl', ($rootScope, $scope, $routeParams, $http) ->
  angular.extend($scope, $routeParams)
  $scope.restRequestMethod = 'GET'
  $scope.restUri =
    "/#{$scope.resourceType}/#{$scope.resourceLogicalId}/_history?_format=application/json"

  rt = $scope.resourceType
  id = $scope.resourceLogicalId
  $rootScope.menu = $rootScope._menu.slice(0)
  $rootScope.menu.push(url: "/resources/#{rt}", label: rt)
  $rootScope.menu.push(url: "/resources/#{rt}/#{id}", label: cropUuid(id))
  $rootScope.menu.push(url: "/resources/#{rt}/#{id}/history", label: 'History', icon: 'fa-history')

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
