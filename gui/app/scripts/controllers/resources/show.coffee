'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesShowCtrl', ($scope, $routeParams, $http, $location) ->
    $scope.resourceType      = $routeParams.resourceType
    $scope.resourceTypeLabel = $scope.resourceType
    $scope.resourceLogicalId = $routeParams.resourceLogicalId
    $scope.resourceLabel     = $scope.resourceLogicalId
    $scope.restRequestMethod = 'GET'
    $scope.restUri =
      "/#{$scope.resourceType}/#{$scope.resourceLogicalId}?_format=application/json"

    $http.get($scope.restUri).success (data, status, headers, config) ->
      $scope.resource = angular.toJson(angular.fromJson(data), true)

    $scope.destroy = ->
      if window.confirm("Destroy #{$scope.resourceTypeLabel} #{$scope.resourceLabel}?")
        $http.delete($scope.restUri).success (data, status, headers, config) ->
          $location.path("/resources/#{$scope.resourceType}")
