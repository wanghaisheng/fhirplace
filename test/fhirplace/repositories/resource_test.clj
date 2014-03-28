(ns fhirplace.repositories.resource-test
  (:require [fhirplace.repositories.resource :refer :all]
            [fhirplace.test-helper :refer :all]
            [midje.sweet :refer :all]))

(deffacts "`resource-types` function returns sequence of all Resources availabe in FhirBase"
  (def resources (resource-types test-db))

  (contains? resources "Patient") => truthy
  (contains? resources "Order") => truthy
  (contains? resources "Encounter") => truthy
  (contains? resources "SomeUnknownResource") => falsey)

(deffacts "`select' fn returns resource as Clojure data"
  (let [patient (fixture "patient")
        patient-id (insert test-db patient)]
    patient-id =not=> nil

    (let [found-patient (select test-db "Patient" patient-id)]
      (:resourceType found-patient) => "Patient"
      (:name found-patient) => (:name patient))))

(deffacts "`exists?' should return exitence of resource"
  (fact "TRUE if resource exists"
    (let [patient (fixture "patient")
          patient-id (insert test-db patient)]
      (exists? test-db patient-id) => true))

  (fact "FALSE if resouce non-exists"
    (exists? test-db (make-uuid)) => false))

