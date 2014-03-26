(ns fhirplace.core
  (:require [clojure.java.jdbc :as sql]))

(defn resource-types [db-spec]
  (set
    (map :path
      (sql/query db-spec ["SELECT DISTINCT(path[1]) FROM meta.resource_elements"]))))

(defn insert-resource [db-spec resource]
  (:insert_resource
    (first
      (sql/query db-spec [(str "SELECT fhir.insert_resource('"
                               resource
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

(defn delete-resource [db-spec resource-id]
  (sql/execute! db-spec [(str "DELETE FROM fhir.resource WHERE _id = '" resource-id "'")]))

(defn update-resource [db-spec resource-id resource]
  (sql/query db-spec [(str "SELECT fhir.update_resource('"
                              resource-id
                              "','"
                              resource
                              "'::json)::varchar")]))

(defn select-history [db-spec id]
  (sql/query db-spec [(str "SELECT _id::varchar as _version_id FROM fhir.resource WHERE _id = '" id "'")]))
