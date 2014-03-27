(ns fhirplace.util-test
  (:require [fhirplace.util :refer :all]
            [fhirplace.test-helper :refer :all]
            [midje.sweet :refer :all]))

(def some-data {:name [{:family ["Bor"]
                        :given ["Roelof Olaf"]
                        :period nil
                        :prefix ["Drs."]
                        :suffix ["PDEng."]
                        :text "Roel"
                        :use "official"}]})

(def patient-data (fixture "patient"))

(fact "`discard-nils' function discards nil values from hashmap"
  (first (:name (discard-nils some-data))) =not=> (contains {:period nil})
  (discard-nils patient-data) => patient-data)
