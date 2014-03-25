(ns fhirplace.core-test
  (:use midje.sweet)
  (:require [fhirplace.core :refer :all]
            [fhirplace.db :as db]))

(fact "`resource-types` function returns sequence of all Resources availabe in FhirBase"
  (def resources (resource-types (db/conn)))

  (contains? resources "Patient") => truthy
  (contains? resources "Order") => truthy
  (contains? resources "Encounter") => truthy
  (contains? resources "SomeUnknownResource") => falsey)
