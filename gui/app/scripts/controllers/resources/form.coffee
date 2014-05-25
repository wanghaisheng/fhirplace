'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesFormCtrl',
  ($scope, $http, $location) ->

    $scope.save = ->
      if $scope.form.$valid
        $http({
          method: $scope.restRequestMethod,
          url: $scope.restUri,
          data: $scope.resource.prettyData,
          headers: {'Content-Location': $scope.resourceContentLocation}
        }).success (data, status, headers, config) ->
          $location.path("/resources/#{$scope.resourceType}")
      else
        # Show all invalid fields.
        # Black magic) <http://stackoverflow.com/questions/18776496/angular-js-validation-programatically-set-form-fields-properties#18778314>.
        angular.forEach $scope.form.$error, (value, key) ->
          type = $scope.form.$error[key]
          angular.forEach type, (item) ->
            item.$dirty    = true
            item.$pristine = false
