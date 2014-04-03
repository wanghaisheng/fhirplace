(ns fhirplace.resources.history-test
  (:use midje.sweet)
  (:require [fhirplace.resources.history :as h]
            [fhirplace.system :as sys]))

(def test-system (sys/create :test))

(facts "`build-history'"
  (let [entries [{:last_modified_date "2013-02-03"}
                 {:last_modified_date "2012-02-02"
                  :state "deleted"}]
        history (h/build-history entries test-system)]

    history => (contains {:resourceType "Bundle"})
    history => (contains {:title "History of Resource"})
    history => (contains {:updated "2013-02-03"})
    history => (contains {:entry anything})
    history => (contains {:totalResults 2})))
  
(facts "`build-entry'"
  (let [entry {:last_modified_date "2013-01-01"
               :id 1111
               :version_id 2222
               :json {:resourceType "Patient"
                      :other-patient-fields "and values"}}
        entry-res (h/build-entry entry test-system)]

    entry-res => (contains {:title "Resource of type Patient, with id = 1111 and version-id = 2222"})
    (first (:link entry-res)) => (contains {:rel "self" :href #"https?://.+/_history/.+"})
    entry-res => (contains {:id #"https?://.+"})
    entry-res => (contains {:content {:other-patient-fields "and values"
                                                :resourceType "Patient"}})))
