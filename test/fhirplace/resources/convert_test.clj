(ns fhirplace.resources.convert-test
  (:use midje.sweet)
  (:require
    [fhirplace.resources.convert :as c]
    [fhirplace.resources.validation :as v]
    [saxon :as xml]
    ))

(def pt-json
  (slurp "test/fixtures/patient.json"))

(def pt-xml
  (c/json->xml pt-json))

(def pt-dom
  (xml/compile-xml pt-xml))

(fact "handling resource text (Narrative)"
      (xml/query "/f:Patient/f:text"
                 {:f "http://hl7.org/fhir"}
                 pt-dom) =not=> nil)

(spit "tmp/res.xml"  pt-xml)

(fact
  (v/errors "Patient" pt-xml)=> nil)
