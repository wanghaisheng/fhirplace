'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesIndexCtrl', ($scope, $routeParams, $http) ->
    $scope.resourceType  = $routeParams.resourceType
    $scope.resourceLabel = $scope.resourceType

    $http.get("/#{$scope.resourceType}/_search?_format=application/json").
      success (data, status, headers, config) ->
        console.log 'data', data
        $scope.collection = data
