'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesEditCtrl', ($scope, $http) ->
    $scope.restRequestMethod = 'PUT'
    $scope.restUri =
      "/#{$scope.resourceType}/#{$scope.resourceLogicalId}?_format=application/json"
