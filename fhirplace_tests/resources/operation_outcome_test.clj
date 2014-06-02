(ns fhirplace.resources.operation-outcome-test
  (:use midje.sweet)
  (:require [fhirplace.resources.operation-outcome :as opout]))

(defn mk-opout [issues]
  (opout/build-operation-outcome issues))

(facts "About OperationOutcome constructor"
  (mk-opout [{:severity "unknown" :details "bang"}]) => nil
  (mk-opout []) => nil

  (let [issues [{:severity "warning" :details "first"}
                {:severity "information" :details "second"}]
        opout (mk-opout issues)]

    (count (:issue opout)) => 2
    (:issue opout) => issues
    (keys opout) => [:issue :resourceType])

  (:issue (opout/build-operation-outcome "fatal" "World is broken")) => (contains {:severity "fatal" :details "World is broken"}))
