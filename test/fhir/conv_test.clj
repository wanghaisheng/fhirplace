(ns fhir.conv-test
  (:require
    [midje.sweet :refer :all]
    [fhir.conv :as f]))

(def json-str (slurp "test/fixtures/patient.json"))
(def xml-str  (slurp "test/fixtures/patient.xml"))

(fact from-string
      (str (class (f/from-json json-str))) => "class org.hl7.fhir.instance.model.Patient"
      (str (class (f/from-xml xml-str))) => "class org.hl7.fhir.instance.model.Patient"
      ; (f/to-json (f/from-json (f/to-json (f/from-json json-str)))) => (f/to-json (f/from-json json-str))
      ; (f/to-xml (f/from-xml (f/to-xml (f/from-xml xml-str)))) => (f/to-xml (f/from-xml xml-str))
      ; (-> json-str f/from-json f/to-json) => (-> xml-str f/from-xml f/to-json)
      )
