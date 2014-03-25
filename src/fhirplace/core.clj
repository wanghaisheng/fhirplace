(ns fhirplace.core
  (:require [clojure.java.jdbc :as sql]))

(def db-spec {})

(defn resource-types []
  (set
    (map :path
      (sql/query db-spec ["SELECT DISTINCT(path[1]) FROM meta.resource_elements"]))))

(defn insert-patient [patient]
  (:insert_resource
    (first
      (sql/query db-spec [(str "SELECT fhir.insert_resource('"
                               patient
                               "'::json)::varchar")]))))

(defn clear-resources []
  (sql/execute! db-spec ["DELETE FROM fhir.resource"]))

(defn select-resource [db-spec resource-type id]
    (first
      (sql/query db-spec [(str "SELECT 2+2")])))
