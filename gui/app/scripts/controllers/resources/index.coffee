'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesIndexCtrl', ($scope, $routeParams, $http) ->
    $scope.resourceType      = $routeParams.resourceType
    $scope.resourceTypeLabel = $scope.resourceType
    $scope.restRequestMethod = 'GET'
    $scope.restUri =
      "/#{$scope.resourceType}/_search?_format=application/json"

    $http.get($scope.restUri).success (data, status, headers, config) ->
      $scope.resources = data.map (resource) ->
        resource.prettyData = angular.toJson(
          angular.fromJson(resource.data),
          true
        )
        resource
