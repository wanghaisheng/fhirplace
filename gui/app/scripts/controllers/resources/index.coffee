'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesIndexCtrl', ($scope, $routeParams, $http) ->
    $scope.resourceType      = $routeParams.resourceType
    $scope.resourceTypeLabel = $scope.resourceType

    $http.get("/#{$scope.resourceType}/_search?_format=application/json").
      success (data, status, headers, config) ->
        $scope.collection = data
