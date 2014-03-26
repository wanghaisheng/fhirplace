(ns fhirplace.repositories.resource-test
  (:use midje.sweet)
  (:require [fhirplace.repositories.resource :refer :all]))

#_(fact "`resource-types` function returns sequence of all Resources availabe in FhirBase"
  (def resources (resource-types (db/conn)))

  (contains? resources "Patient") => truthy
  (contains? resources "Order") => truthy
  (contains? resources "Encounter") => truthy
  (contains? resources "SomeUnknownResource") => falsey)
