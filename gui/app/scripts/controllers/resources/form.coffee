'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesFormCtrl',
  ($scope, $http, $location) ->

    $scope.save = ->
      if $scope.form.$valid
        $http.post($scope.restUri, $scope.resource.json)
          .success((data, status, headers, config) ->
            console.log 'me hapy'
            $location.path("/resources/#{$scope.resourceType}")
          ).error (data, status, headers, config) ->
            console.log 'digital gods do not love our json post'
      else
        # Show all invalid fields.
        # Black magic) <http://stackoverflow.com/questions/18776496/angular-js-validation-programatically-set-form-fields-properties#18778314>.
        angular.forEach $scope.form.$error, (value, key) ->
          type = $scope.form.$error[key]
          angular.forEach type, (item) ->
            item.$dirty    = true
            item.$pristine = false

    $scope.validateUri =
      "/#{$scope.resourceType}/_validate?_format=application/json"
    $scope.validate = ->
      console.log 'validating'
      if $scope.form.$valid
        console.log $scope.validateUri
        console.log $scope.resource.json
        $scope.resourceValidation = 'Validating ...'
        $http.post($scope.validateUri, $scope.resource.json)
          .success((data, status, headers, config) ->
            console.log 'me happy to'
            if data
              $scope.resourceValidation = angular.toJson(
                angular.fromJson(data),
                true
              )
            else
              $scope.resourceValidation = 'Everything is good'
          ).error (data, status, headers, config) ->
            console.log 'not happy at all'
            $scope.resourceValidation = angular.toJson(
              angular.fromJson(data),
              true
            )
            console.log data
            console.log status
