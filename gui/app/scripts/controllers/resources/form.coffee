'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesFormCtrl',
  ($scope,
   $http,
   $location,
   $routeParams) ->
    $scope.save = ->
      if $scope.form.$valid
        $http.post("/#{$routeParams.resourceType}", $scope.rawResource).
          success((data, status, headers, config) ->
            console.log 'me hapy'
            $location.path("/resources/#{$routeParams.resourceType}")
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
