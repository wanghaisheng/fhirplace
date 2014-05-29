(ns fhir-test
  (:require
    [midje.sweet :refer :all]
    [fhir :as f]))

(def x (slurp "test/fixtures/patient.xml"))
(f/parse x)
(f/serialize :xml (f/parse x))
(f/errors (f/parse x))

(def jx (slurp "test/fixtures/patient.json"))
(f/parse jx)

(fact "test conformance"
  (f/conformance) =not=> nil)
