(ns fhir.schematron-test
  (:use midje.sweet)
  (:require
    [fhir.schematron :as s]
    [clojure.java.io :as io]
    [saxon :as xml]))

(def pt-sch
  (s/compile-sch "fhir/patient.sch"))

(facts
  (pt-sch
    (slurp "test/fixtures/patient.xml")) => nil

  (pt-sch
    (slurp "test/fixtures/patient-invalid-schematron.xml"))
  => (one-of map?))
