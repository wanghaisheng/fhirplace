(ns fhirplace.interactions.system
  (:use ring.util.response))

(defn conformance
  "Get a conformance statement for the system"
  [request]

  (->
    (response {:resourceType "Conformance"
               :name         "Some Conformance"
               :publisher    "FHIRPlace 0.1"
               :telecom      []
               :description  "Some useless text"
               :date         "2012-12-12"
               :fhirVersion  "DSTU"
               :acceptUnknown false
               :format        ["json"]
               :rest          []
               })))

(defn transaction
  "Update, create or delete a set of resources as a single transaction"
  [request]
  (-> (response "Transaction")
    (content-type "text/json")))

(defn history
  "Retrieve the update history for all resources"
  [request]
  (-> (response "History")
    (content-type "text/json")))

(defn search
  "Search across all resource types based on some filter criteria"
  [request]
  (-> (response "History")
    (content-type "text/json")))
