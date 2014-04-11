(ns fhirplace.resources.parse-test
  (:use midje.sweet)
  (:require [fhirplace.resources.parse :as p]
            [clojure.data.xml :as xml]))

(facts "Parsing and validating resource from XML"
  (fact "should return parsed resource when source XML is valid"
    (let [result (p/parse-resource-with-validations (slurp "test/fixtures/patient.xml") :xml)]
      (first result) =not=> nil
      (last result) => [])))
