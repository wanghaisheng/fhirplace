'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesShowCtrl', ($scope, $routeParams, $http) ->
    $scope.resourceType       = $routeParams.resourceType
    $scope.resourceTypeLabel = $scope.resourceType
    $scope.resourceLogicalId = $routeParams.resourceLogicalId
    $scope.resourceLabel     = $scope.resourceLogicalId
    $scope.restRequestMethod = 'GET'
    $scope.restUri =
      "/#{$scope.resourceType}/#{$scope.resourceLogicalId}?_format=application/json"

    $http.get($scope.restUri).success (data, status, headers, config) ->
      $scope.resource = {
        prettyData: angular.toJson(angular.fromJson(data), true)
      }
      $scope.resourceContentLocation = headers('content-location')
