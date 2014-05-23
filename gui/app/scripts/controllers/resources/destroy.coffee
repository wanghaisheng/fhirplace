'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesDestroyCtrl',
  ($scope, $routeParams, $http, $location) ->
    $scope.restRequestMethod = 'DELETE'
    $scope.restUri =
      "/#{$scope.resourceType}/#{$scope.resourceLogicalId}?_format=application/json"

    $scope.destroy = ->
      if window.confirm("Destroy #{$scope.resourceTypeLabel} #{$scope.resourceLabel}?")
        $http.delete($scope.restUri).success (data, status, headers, config) ->
          $location.path("/resources/#{$scope.resourceType}")
