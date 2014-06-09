'use strict'

app = angular.module 'fhirplaceGui', [
  'ngCookies',
  'ngAnimate',
  'ngSanitize',
  'ngRoute',
  "ui.codemirror"
], ($routeProvider) ->
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
      .when '/resources/:resourceType/:resourceLogicalId/history',
        templateUrl: '/views/resources/history.html'
        controller: 'ResourcesHistoryCtrl'
      .otherwise
        redirectTo: '/'

defaultMenu = [{url: '/conformance', label: 'Conformance'}]
menu = (args...)->
  defaultMenu.slice(0).concat(args)

app.run ($rootScope)->
  $rootScope.menu = menu()
  $rootScope.$watch 'progress', (v)->
    return unless v && v.success
    delete $rootScope.error
    $rootScope.loading = 'Loading'
    $rootScope.progressCls = 'prgrss'
    v.success (vv, status, _, req)->
       $rootScope.loading = null
       $rootScope.success = "#{new Date()} - #{req.method} #{req.url}"
       delete $rootScope.progressCls
       console.log('progress success', req)
     .error (vv, status, _, req)->
       $rootScope.loading = null
       $rootScope.error = vv || "Server error #{status} while loading:  #{req.url}"
       console.log('progress error', arguments)
       delete $rootScope.progressCls

cropUuid = (id)->
  return "ups no uuid :(" unless id
  sid = id.substring(id.length - 6, id.length)
  "...#{sid}"

app.filter 'uuid', ()-> cropUuid

keyComparator = (key)->
 (a, b) ->
   switch
     when a[key] < b[key] then -1
     when a[key] > b[key] then 1
     else 0

app.controller 'ConformanceCtrl', ($rootScope, $scope, $http) ->
  $scope.restRequestMethod = 'GET'
  $scope.restUri = '/metadata?_format=application/json'
  $rootScope.menu = menu()

  $rootScope.progress = $http.get($scope.restUri)
    .success (data, status, headers, config) ->
      $scope.resources = data.rest[0].resource.sort(keyComparator('type'))
      delete data.rest
      delete data.text
      $scope.conformance = data

app.controller 'ResourcesIndexCtrl', ($rootScope, $scope, $routeParams, $http) ->
  angular.extend($scope, $routeParams)
  $scope.restRequestMethod = 'GET'
  rt = $scope.resourceType

  $scope.query = {}
  $scope.restUri = "/#{rt}/_search?_format=application/json"

  $rootScope.progress = $http.get("/Profile/#{rt}").success (data, status, headers, config)->
    $scope.profile = data

  $rootScope.menu = menu(
    {url: "/resources/#{rt}", label: rt, active: true},
    {url: "/resources/#{rt}/new", label: "New", icon: "fa-plus"})

  $scope.search = ()->
    $rootScope.progress = $http.get($scope.restUri, {params: angular.copy($scope.query)})
      .success (data, status, headers, config) ->
        $scope.searchUri = config
        $scope.resources = data.entry

  $scope.search()

app.controller 'ResourcesNewCtrl', ($rootScope, $scope, $routeParams, $http, $location) ->
  angular.extend($scope, $routeParams)

  rt = $scope.resourceType

  $rootScope.menu = menu(
    {url: "/resources/#{rt}", label: rt},
    {url: "/resources/#{rt}/new", label: "New", icon: "fa-plus", active: true})


  $scope.restRequestMethod = 'POST'
  $scope.restUri = "/#{$scope.resourceType}?_format=application/json"
  $scope.resource = {}

  headers = {'Content-Location': $scope.resourceContentLocation}
  $scope.save = ->
    $rootScope.progress = $http(method: $scope.restRequestMethod, url: $scope.restUri, data: $scope.resource.content, headers: headers)
      .success (data, status, headers, config) ->
        $location.path("/resources/#{$scope.resourceType}")

  $scope.validate = ()->
    url = "/#{rt}/_validate?_format=application/json"
    $rootScope.progress = $http.post(url, $scope.resource.content)
      .success (data)-> alert('Valid input')

app.controller 'ResourceCtrl', ($rootScope, $scope, $routeParams, $http, $location) ->
  angular.extend($scope, $routeParams)
  $scope.restRequestMethod = 'GET'
  $scope.restUri = "/#{$scope.resourceType}/#{$scope.resourceLogicalId}?_format=application/json"

  rt = $scope.resourceType
  id = $scope.resourceLogicalId
  $rootScope.menu = menu(
    {url: "/resources/#{rt}", label: rt},
    {url: "/resources/#{rt}/#{id}", label: cropUuid(id), active: true},
    {url: "/resources/#{rt}/#{id}/history", label: 'History', icon: 'fa-history'})

  loadResource = ()->
    $rootScope.progress = $http.get($scope.restUri).success (data, status, headers, config) ->
      $scope.resource = {
        content: angular.toJson(angular.fromJson(data), true)
      }
      console.log(headers('Content-Location'))
      $scope.resourceContentLocation = headers('Content-Location')
      throw "content location required" unless $scope.resourceContentLocation

  loadResource()
  $scope.save = ->
    opts = {
      method: "PUT",
      url: $scope.restUri,
      data: $scope.resource.content,
      headers: {'Content-Location': $scope.resourceContentLocation}
    }
    $rootScope.progress = $http(opts)
      .success (data,status,headers,req)->
        $scope.resourceContentLocation = headers('content-location')


  $scope.destroy = ->
    if window.confirm("Destroy #{$scope.resourceTypeLabel} #{$scope.resourceLabel}?")
      $rootScope.progress = $http.delete($scope.restUri).success (data, status, headers, config) ->
        $location.path("/resources/#{$scope.resourceType}")

  $scope.validate = ()->
    url = "/#{rt}/_validate?_format=application/json"
    $rootScope.progress = $http.post(url, $scope.resource.content)
      .success (data)-> alert('Valid input')

app.controller 'ResourcesHistoryCtrl', ($rootScope, $scope, $routeParams, $http) ->
  angular.extend($scope, $routeParams)
  $scope.restRequestMethod = 'GET'
  $scope.restUri =
    "/#{$scope.resourceType}/#{$scope.resourceLogicalId}/_history?_format=application/json"

  rt = $scope.resourceType
  id = $scope.resourceLogicalId
  $rootScope.menu = menu(
    {url: "/resources/#{rt}", label: rt},
    {url: "/resources/#{rt}/#{id}", label: cropUuid(id)},
    {url: "/resources/#{rt}/#{id}/history", label: 'History', icon: 'fa-history', active: true})

  $rootScope.progress = $http.get($scope.restUri).success (data, status, headers, config) ->
    $scope.entries = data.entry
    $scope.history  = data
    delete $scope.history.entry
