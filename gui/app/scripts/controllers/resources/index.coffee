'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesIndexCtrl', ($scope, $routeParams) ->
    $scope.resourceType  = $routeParams.resourceType
    $scope.resourceLabel = $routeParams.resourceType
