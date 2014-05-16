'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesIndexCtrl', ($scope, $routeParams) ->
    $scope.resource = $routeParams.resourceId
