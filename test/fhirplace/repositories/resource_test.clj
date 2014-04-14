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
  (let
    [patient (fixture "patient")
     patient-id (repo/insert test-db patient)
     {found-patient :data} (repo/select-latest-version
                             test-db "Patient" patient-id)]

    patient-id =not=> nil
    (:resourceType found-patient) => "Patient"
    (:name found-patient) => (:name patient)))

(deffacts "`delete'"
  (let [patient (fixture "patient")
        patient-id (repo/insert test-db patient)]
    (repo/delete test-db patient-id)
    (repo/select-latest-version test-db "Patient" patient-id) => nil))

(deffacts "`update'"
  (let [patient (fixture "patient")
        patient-id (repo/insert test-db patient)]
    (repo/update test-db patient-id (assoc patient :active false))
    (repo/select-latest-version test-db "Patient" patient-id) => (contains {:data
                                                                            (contains {:active false})})))

(deffacts "`exists?' should return exitence of resource"
  (fact "TRUE if resource exists"
        (let [patient (fixture "patient")
              patient-id (repo/insert test-db patient)]
          (repo/exists? test-db patient-id) => true))

  (fact "FALSE if resouce non-exists"
        (repo/exists? test-db (make-uuid)) => false)

  (fact "FALSE if resouce was deleted"
        (let [patient-id (repo/insert test-db (fixture "patient"))]
          (repo/delete test-db patient-id)
          (repo/exists? test-db (make-uuid)) => false)))

(deffacts "`deleted?'"
  (fact "FALSE if resource not deleted"
        (let [patient-id (repo/insert test-db (fixture "patient"))]
          (repo/deleted? test-db patient-id) => false

          (fact "TRUE if resource was deleted"
                (repo/delete test-db patient-id)
                (repo/deleted? test-db patient-id) => true))))

(deffacts "`select-latest'"
  (let [patient (fixture "patient")
        id (repo/insert test-db patient)
        vid (repo/select-latest-version-id test-db "Patient" id)]

    (:data
      (repo/select-version test-db "Patient" id vid)) => (contains
                                                           (select-keys patient
                                                                        [:name :active :resourceType :organization]))))

(deffacts "`select-latest-version'"
  (let [patient (fixture "patient")
        id (repo/insert test-db patient)]

    (:data
      (repo/select-latest-version test-db "Patient" id)) => (contains
                                                              (select-keys patient
                                                                           [:name :active :resourceType :organization]))))
