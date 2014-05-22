'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesEditCtrl', ($scope, $routeParams) ->
    $scope.resourceType      = $routeParams.resourceType
    $scope.resourceLogicalId = $routeParams.resourceLogicalId
    $scope.resourceTypeLabel = $routeParams.resourceType + ' ' +
      $routeParams.resourceLogicalId
    $scope.resource = {
      json: JSON.stringify({
        ggg: $scope.resourceLogicalId, xyz: 'jjj'
      })
    }
