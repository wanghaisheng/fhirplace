(ns fhirplace.core
  (:require [clojure.java.jdbc :as sql]))

(defn resource-types [db-spec]
  (set
    (map :path
      (sql/query db-spec ["SELECT DISTINCT(path[1]) FROM meta.resource_elements"]))))

(defn insert-patient [db-spec patient]
  (:insert_resource
    (first
      (sql/query db-spec [(str "SELECT fhir.insert_resource('"
                               patient
                               "'::json)::varchar")]))))

(defn clear-resources [db-spec]
  (sql/execute! db-spec ["DELETE FROM fhir.resource"]))

(defn select-resource [db-spec resource-type id]
  (:json
    (first
      (sql/query db-spec [(str "SELECT json::text"
                               " FROM fhir.view_" (.toLowerCase resource-type)
                               " WHERE _id = '" id "'"
                               " LIMIT 1")]))))
