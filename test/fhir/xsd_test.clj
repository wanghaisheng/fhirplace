(ns fhir.xsd-test
  (:use midje.sweet)
  (:require [fhir.xsd :as fx]))


(def pt-validator
  (fx/mk-validator "fhir/patient.xsd"))

(fact
  (pt-validator
    (slurp "test/fixtures/patient.xml")) => nil

  (pt-validator
    (slurp "test/fixtures/invalid-patient.xml"))
  => (contains "org.xml.sax.SAXParseException"))
