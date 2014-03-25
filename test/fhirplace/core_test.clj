(ns fhirplace.core-test
  (:use midje.sweet)
  (:require [fhirplace.core :refer :all]))

(fact "`resource-types` function returns sequence of all Resources availabe in FhirBase"
  (def resources (resource-types))
  (contains? resources "Patient") => truthy
  (contains? resources "Order") => truthy
  (contains? resources "Encounter") => truthy
  (contains? resources "SomeUnknownResource") => falsey)
