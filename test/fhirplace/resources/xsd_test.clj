(ns fhirplace.resources.xsd-test
  (:use midje.sweet)
  (:require [fhirplace.resources.xsd :as x]
            [clojure.java.io :as io]))


(def pt-validator
  (x/mk-validator "fhir/patient.xsd"))

(fact
  (pt-validator
    (slurp "test/fixtures/patient.xml")) => nil

  (pt-validator
    (slurp "test/fixtures/invalid-patient.xml"))
  => (contains "Invalid content was found"))
