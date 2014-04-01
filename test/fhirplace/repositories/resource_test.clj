(ns fhirplace.repositories.resource-test
  (:require [fhirplace.repositories.resource :as repo]
            [fhirplace.test-helper :refer :all]
            [midje.sweet :refer :all]))

(deffacts "`resource-types` function returns sequence of all Resources availabe in FhirBase"
  (def resources (repo/resource-types test-db))

  (contains? resources "Patient") => truthy
  (contains? resources "Order") => truthy
  (contains? resources "Encounter") => truthy
  (contains? resources "SomeUnknownResource") => falsey)

(deffacts "`select' fn returns resource as Clojure data"
  (let [patient (fixture "patient")
        patient-id (repo/insert test-db patient)]
    patient-id =not=> nil

    (let [found-patient (repo/select test-db "Patient" patient-id)]
      (:resourceType found-patient) => "Patient"
      (:name found-patient) => (:name patient))))

(deffacts "`delete'"
  (let [patient (fixture "patient")
        patient-id (repo/insert test-db patient)]
    (repo/delete test-db patient-id)
    (repo/select test-db "Patient" patient-id) => nil))

(deffacts "`update'"
  (let [patient (fixture "patient")
        patient-id (repo/insert test-db patient)]
    (repo/update test-db patient-id (assoc patient :active false))
    (repo/select test-db "Patient" patient-id) => (contains {:active false})))

(deffacts "`exists?' should return exitence of resource"
  (fact "TRUE if resource exists"
    (let [patient (fixture "patient")
          patient-id (repo/insert test-db patient)]
      (repo/exists? test-db patient-id) => true))

  (fact "FALSE if resouce non-exists"
    (repo/exists? test-db (make-uuid)) => false))

(deffacts "`select-version'"
  (let [patient (fixture "patient")
        patient-id (repo/insert test-db patient)
        {version-id :version_id} (first (repo/select-history test-db "Patient" patient-id))]

      (repo/select-version test-db "Patient" patient-id version-id) => (contains 
                                                                         (select-keys patient 
                                                                                      [:name :active :resourceType :organization]))))
