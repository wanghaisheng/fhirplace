(ns fhirplace.core
  (:require [clojure.java.jdbc :as sql]))

(def db-spec
  {:subprotocol "postgresql"
   :subname "//127.0.0.1:5433/fhirbase"
   :user "vagrant"})

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

(defn select-resource [resource-type id]
  (:json
    (first
      (sql/query db-spec [(str "SELECT json::text"
                               " FROM fhir.view_" (.toLowerCase resource-type)
                               " WHERE _id = '" id "'"
                               " LIMIT 1")]))))
