'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesShowCtrl', ($scope, $routeParams, $http) ->
    $scope.resourceType      = $routeParams.resourceType
    $scope.resourceLogicalId = $routeParams.resourceLogicalId
    $scope.resourceTypeLabel = $scope.resourceType

    $http.get(
      "/#{$scope.resourceType}/#{$scope.resourceLogicalId}?_format=application/json"
    ).success (data, status, headers, config) ->
      $scope.resource = angular.toJson(angular.fromJson(data), true)

    $http.get(
      "/#{$scope.resourceType}/#{$scope.resourceLogicalId}/_history?_format=application/json"
    ).success (data, status, headers, config) ->
      $scope.history = data.entry
      $scope.pretty  = angular.toJson(angular.fromJson(data), true)
