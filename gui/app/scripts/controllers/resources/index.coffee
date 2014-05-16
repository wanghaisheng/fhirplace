'use strict'

angular.module('fhirplaceSpaUi')
  .controller 'ResourcesIndexCtrl', ($scope, $routeParams) ->
    $scope.resource = $routeParams.resourceId
