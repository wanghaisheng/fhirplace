'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesHistoryCtrl', ($scope, $routeParams, $http) ->
    $scope.resourceType      = $routeParams.resourceType
    $scope.resourceTypeLabel = $scope.resourceType
    $scope.resourceLogicalId = $routeParams.resourceLogicalId
    $scope.resourceLabel     = $scope.resourceLogicalId

    $http.get(
      "/#{$scope.resourceType}/#{$scope.resourceLogicalId}/_history?_format=application/json"
    ).success (data, status, headers, config) ->
      $scope.resourceHistory  = angular.toJson(angular.fromJson(data), true)
      $scope.resourceVersions = data.entry
