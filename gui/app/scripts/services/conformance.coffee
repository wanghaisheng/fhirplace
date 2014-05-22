'use strict'

angular.module('fhirplaceGui')
  .factory 'conformance',
  ($http) ->

    foo = ->
      promise = $http({method: 'GET', url: '/metadata?_format=application/json'})
        .then((data, status, headers, config) ->
          console.log data
          data
        )
      promise

    {
      name: ->
        foo.name

      resources: ->
        foo.resources
    }
