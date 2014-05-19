'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesNewCtrl', ($scope, $routeParams) ->
    $scope.resourceType  = $routeParams.resourceType
    $scope.resourceLabel = $routeParams.resourceType
    $scope.rawResource   = {}
