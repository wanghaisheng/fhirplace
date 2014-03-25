(ns fhirplace.resources.conformance-test
  (:use midje.sweet
        fhirplace.resources.conformance)
  (:require [fhirplace.resources.conformance :refer :all]))

(defn build-conformance [] {})

(fact "Conformance builder builds Conformance resource"
  (:resourceType (build-conformance)) => "Conformance")

(let [conformance (build-conformance)]
  (fact "A Conformance statement SHALL have at least one of rest, messaging or document"
    (or (:rest conformance) (:messaging conformance) (:document conformance)) =not=> nil)

  (fact " A Conformance statement SHALL have at least one of description, software, or implementation"
    (map (fn [k] (count (k conformance))) [:software :implementation :description])))

(fact "Exist validator validates presence of attribute in object"
  (let [foo-key-exist? (v-exist :foo)]
    (foo-key-exist? {} []) => [{:type :exist :key :foo}]))
