'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesHistoryCtrl', ($scope, $routeParams, $http) ->
    $scope.resourceType      = $routeParams.resourceType
    $scope.resourceTypeLabel = $scope.resourceType
    $scope.resourceLogicalId = $routeParams.resourceLogicalId
    $scope.resourceLabel     = $scope.resourceLogicalId
    $scope.restRequestMethod = 'GET'
    $scope.restUri =
      "/#{$scope.resourceType}/#{$scope.resourceLogicalId}/_history?_format=application/json"

    $http.get($scope.restUri).success (data, status, headers, config) ->
      $scope.resourceHistory  = angular.toJson(angular.fromJson(data), true)
      $scope.resourceVersions = data.entry
