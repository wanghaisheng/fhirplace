(ns fhirplace.resources.history-test
  (:use midje.sweet)
  (:require [fhirplace.resources.history :as h]
            [fhirplace.system :as sys]
            [fhirplace.util :as util]))

(def test-system (sys/create :test))
(def datetime? (partial instance? java.util.Date))
(def uri-regex #"https?://.+")
(defn timestamp []
  (-> (java.util.Date.)
      (.getTime)
      (java.sql.Timestamp.)))

(facts "`build-history'"
       (let [entries [{:last_modified_date (timestamp)}
                      {:last_modified_date (timestamp)
                       :state "deleted"}]
             history (h/build-history entries test-system)]

         history => (contains {:resourceType "Bundle"})
         history => (contains {:id string?})
         (:author history) => (just {:name string? :uri uri-regex})
         (:link history)
         => (just #{{:rel "fhir-base" :href (util/cons-url test-system)}
                    #_{:rel "self", :href uri}})
         
         history => (contains {:title "History of Resource"})
         history => (contains {:updated datetime?})
         history => (contains {:entry anything})
         history => (contains {:totalResults 2})))


(facts "`build-entry'"
  (fact "Updated resource"
    (let [entry {:last_modified_date (timestamp)
                 :id 1111
                 :state "updated"
                 :version-id 2222
                 :json {:resourceType "Patient"
                        :other-patient-fields "and values"}}
          entry-res (h/build-entry entry test-system)]

      entry-res => (contains {:title "Resource of type Patient, with id = 1111 and version-id = 2222"})
      (first (:link entry-res)) => (contains {:rel "self" :href #"https?://.+/_history/.+"})
      entry-res => (contains {:id uri-regex})
      entry-res => (contains {:content {:other-patient-fields "and values"
                                        :resourceType "Patient"}})
      entry-res => (contains {:updated datetime?})
      entry-res => (contains {:published datetime?})))
  (fact "Deleted resource"
    (let [entry {:last_modified_date (timestamp)
                 :id 1111
                 :version-id 2222
                 :state "deleted"
                 :json {:resourceType "Patient" }}
          entry-res (h/build-entry entry test-system)]
      entry-res => (contains {:deleted (timestamp)}))))
