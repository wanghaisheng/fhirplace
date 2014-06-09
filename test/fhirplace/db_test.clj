(ns fhirplace.external-test
  (:use midje.sweet)
  (:require [fhirplace.db :as db]
            [fhir :as f]
            [clojure.test :refer :all]))

(defn fixture [nm]
  (slurp (str "test/fhirplace/fixtures/" nm)))

(def pt (db/-create "Patient" (fixture "patient.json")))

(db/-latest? "Patient" (:logical_id pt) (:version_id pt))

(print (db/-update "Patient" (:logical_id pt) (fixture "patient.json")))
(print (db/-delete "Patient" (:logical_id pt)))

