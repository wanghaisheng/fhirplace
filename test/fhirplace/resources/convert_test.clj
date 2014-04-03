(ns fhirplace.resources.convert-test
  (:use midje.sweet)
  (:require
    [fhirplace.resources.convert :as c]
    [fhirplace.resources.validation :as v]))

(def pt-json
  (slurp "test/fixtures/patient.json"))

; (spit "tmp/res.xml" (c/json->xml pt-json))
; (v/errors "Patient" (c/json->xml pt-json))
