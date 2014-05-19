'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesNewCtrl', ($scope, $routeParams) ->
    $scope.resourceId    = $routeParams.resourceId
    $scope.resourceLabel = $routeParams.resourceId
    $scope.rawResource = { json: "{ foo: 'bar'}" }
