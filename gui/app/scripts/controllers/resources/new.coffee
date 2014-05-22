'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesNewCtrl', ($scope, $routeParams) ->
    $scope.resourceType      = $routeParams.resourceType
    $scope.resourceTypeLabel = $routeParams.resourceType
    $scope.resource       = {}
