(ns fhirplace.resources.history-test
  (:use midje.sweet)
  (:require [fhirplace.resources.history :as h]
            [fhirplace.system :as sys]))

(def test-system (sys/create :test))

(facts "`build-history'"
  (let [entries [{:last-modified-date "2013-02-03"}
                 {:last-modified-date "2012-02-02"
                  :state "deleted"}]
        history (h/build-history entries test-system)]

    history => (contains {:resourceType "Bundle"})
    history => (contains {:title "History of Resource"})
    history => (contains {:updated "2013-02-03"})
    history => (contains {:entry anything})
    history => (contains {:totalResults 2})))
  
(facts "`build-entry'"
  (fact "Updated resource"
    (let [entry {:last-modified-date "2013-01-01"
                 :id 1111
                 :state "updated"
                 :version-id 2222
                 :json {:resourceType "Patient"
                        :other-patient-fields "and values"}}
          entry-res (h/build-entry entry test-system)]

      entry-res => (contains {:title "Resource of type Patient, with id = 1111 and version-id = 2222"})
      (first (:link entry-res)) => (contains {:rel "self" :href #"https?://.+/_history/.+"})
      entry-res => (contains {:id #"https?://.+"})
      entry-res => (contains {:content {:other-patient-fields "and values"
                                        :resourceType "Patient"}})))
  (fact "Deleted resource"
    (let [entry {:last-modified-date "2013-01-01"
                 :id 1111
                 :version-id 2222
                 :state "deleted"
                 :json {:resourceType "Patient" }}
          entry-res (h/build-entry entry test-system)]
      entry-res => (contains {:deleted "2013-01-01"}))))
