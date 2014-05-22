'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesFormCtrl',
  ($scope,
   $http,
   $location,
   $routeParams) ->
    $scope.resourceType = $routeParams.resourceType

    $scope.save = ->
      if $scope.form.$valid
        $http.post(
          "/#{$scope.resourceType}?_format=application/json",
          $scope.rawResource.json
          ).
          success((data, status, headers, config) ->
            console.log 'me hapy'
            $location.path("/resources/#{$scope.resourceType}")
          ).
          error (data, status, headers, config) ->
            console.log 'digital gods do not love our json post'
      else
        # Show all invalid fields.
        # Black magic) <http://stackoverflow.com/questions/18776496/angular-js-validation-programatically-set-form-fields-properties#18778314>.
        angular.forEach $scope.form.$error, (value, key) ->
          type = $scope.form.$error[key]
          angular.forEach type, (item) ->
            item.$dirty    = true
            item.$pristine = false
