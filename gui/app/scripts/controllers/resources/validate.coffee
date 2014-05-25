'use strict'

angular.module('fhirplaceGui')
  .controller 'ResourcesValidateCtrl', ($scope, $http) ->
    $scope.restRequestMethod = 'POST'
    $scope.restUri =
      "/#{$scope.resourceType}/_validate?_format=application/json"

    $scope.validate = ->
      if $scope.form.$valid
        $scope.resourceValidation = 'Validating ...'
        $http.post($scope.restUri, $scope.resource.prettyData)
          .success((data, status, headers, config) ->
            if data
              $scope.resourceValidation = angular.toJson(
                angular.fromJson(data),
                true
              )
            else
              $scope.resourceValidation = 'Everything is good'
          ).error (data, status, headers, config) ->
            $scope.resourceValidation = angular.toJson(
              angular.fromJson(data),
              true
            )
