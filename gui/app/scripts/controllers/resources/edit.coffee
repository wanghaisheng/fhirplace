'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesEditCtrl', ($scope, $routeParams) ->
    $scope.resourceType  = $routeParams.resourceType
    $scope.resourceId    = $routeParams.resourceId
    $scope.resourceLabel = $routeParams.resourceType + ' ' +
      $routeParams.resourceId
    $scope.rawResource = {
      json: JSON.stringify({
        ggg: $routeParams.resourceId, xyz: 'jjj'
      })
    }
