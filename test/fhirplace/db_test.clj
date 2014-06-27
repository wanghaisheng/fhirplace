(ns fhirplace.external-test
  (:use midje.sweet)
  (:require [fhirplace.db :as db]
            [fhir :as f]
            [clojure.test :refer :all]
            [fhirplace.test-helper :as ft]))

#_(print (f/serialize :json (db/-tags)))

((ft/def-db-test integration-test
   (def pt (db/-create "Patient" (ft/fixture "patient.json") "[]"))

   (is (not (nil? pt)))

   (is (db/-latest? "Patient" (:logical_id pt) (:version_id pt)))
   (let [res (db/-search "Patient" {:_sort ["name"]})]
     (is (not (nil? pt))))

   (is (> 0 (count (.getEntryList (db/-search "Patient" {:name "Pete" :_sort ["name"]})))))

   (db/-update "Patient" (:logical_id pt) (fixture "patient.json") "[]")
   (db/-delete "Patient" (:logical_id pt))
   ))

(db/qcall* :read "Patient" "bca2a820-d28d-4eca-928d-3cae7c04de19")
