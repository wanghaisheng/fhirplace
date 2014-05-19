'use strict'

angular.module('fhirplaceGui')
  .controller 'MetadataIndexCtrl', ($scope, $http) ->
    $http.get('/metadata?_format=application/json').
      success((data, status, headers, config) ->
        $scope.name = data.name
        $scope.resources = data.rest[0].resources.sort (a, b) ->
          return -1 if a.type < b.type
          return 1 if a.type > b.type
          0
      ).
      error (data, status, headers, config) ->
        # called asynchronously if an error occurs
        # or server returns response with an error status.
        console.log 'karamba'
