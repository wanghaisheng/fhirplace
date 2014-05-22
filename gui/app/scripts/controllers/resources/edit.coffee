'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesEditCtrl', ($scope, $routeParams) ->
    $scope.resourceType      = $routeParams.resourceType
    $scope.resourceTypeLabel = $scope.resourceType
    $scope.resourceLogicalId = $routeParams.resourceLogicalId
    $scope.restRequestMethod = 'POST'
    $scope.restUri = '?????????????????????'
    $scope.resource = {
      json: JSON.stringify({
        ggg: $scope.resourceLogicalId, xyz: 'jjj'
      })
    }
