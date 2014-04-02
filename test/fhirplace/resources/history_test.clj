(ns fhirplace.resources.history-test
  (:use midje.sweet)
  (:require [fhirplace.resources.history :as h]))


(facts "`build-history'"
  (let [entries [{:version_id "123"
                   :last_modified_date "2013-02-03"}
                  {:version_id "456"
                   :last_modified_date "2012-02-02"
                   :state "deleted"}]
        url "http://localhost/patient/1111/_history"]

    (build-history entries url) => (contains 
                                      {:resourceType "Bundle"
                                       :title "History of Resource"
                                       :updated "2013-02-03"
                                       :link [{:rel "self" :href url}]
                                       :totalResults 2})))
  
(facts "`build-entry'"
  (let [entry {:last_modified_date "2013-01-01"
               :id 1111
               :version_id 2222
               :json {:resourceType "Patient"
                      :other-patient-fields "and values"}}
        entry-map (build-entry entry)]

    (build-entry entry) => (contains {:title "Resource of type Patient, with id = 1111 and version-id = 2222"})
    (build-entry entry) => (contains {:link [{:rel "self" :href "Dummy url"}]})
    (build-entry entry) => (contains {:id "Dummy url"})
    (build-entry entry) => (contains {:content {:other-patient-fields "and values"
                                                :resourceType "Patient"}})))
