'use strict'

angular.module('fhirplaceGui')
  .controller 'MetadataIndexCtrl', ($scope, $http) ->
    $scope.restRequestMethod = 'GET'
    $scope.restUri = '/metadata?_format=application/json'

    $http.get($scope.restUri).success((data, status, headers, config) ->
      $scope.serverName = data.name
      $scope.resourceTypes = data.rest[0].resources.sort (a, b) ->
        return -1 if a.type < b.type
        return 1 if a.type > b.type
        0
    ).error (data, status, headers, config) ->
      # called asynchronously if an error occurs
      # or server returns response with an error status.
      console.log 'karamba'
