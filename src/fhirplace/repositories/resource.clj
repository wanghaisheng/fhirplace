(ns fhirplace.repositories.resource
  (:require [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]
            [fhirplace.util :as util])
  (:refer-clojure :exclude (delete)))

(defn- json-to-string [json-or-string]
  (if (string? json-or-string)
    json-or-string
    (json/write-str json-or-string)))

(defn- run-query [db-spec sql-text]
  (sql/query db-spec [sql-text]))

(defn clean-json [json]
  (-> json
      (json/read-str :key-fn keyword)
      util/discard-indexes
      util/discard-nils))

(defn insert [db-spec resource]
  (:insert_resource
   (first
     (run-query db-spec (str "SELECT fhir.insert_resource('"
                           (json-to-string resource)
                           "'::json)::varchar")))))

(defn resource-types [db-spec]
  (set
    (map :path
      (run-query db-spec "SELECT DISTINCT(path[1]) FROM meta.resource_elements"))))

(defn select-version [db-spec resource-type id vid]
  (let [[version] 
        (run-query db-spec
                   (str "SELECT _last_modified_date as \"last-modified-date\","
                        " json::text"
                        " FROM fhir.view_" (.toLowerCase resource-type) "_history"
                        " WHERE _logical_id = '" id "' and _version_id = '" vid "'"
                        " and _state <> 'deleted'"
                        " LIMIT 1"))]

    (when version
      (update-in version [:json] clean-json))))

(defn select-latest-version [db-spec resource-type id]
  (let [[version]
        (run-query db-spec (str "SELECT _version_id::varchar as \"version-id\","
                                " _last_modified_date::varchar as \"last-modified-date\","
                                " json::text"
                                " FROM fhir.view_" (.toLowerCase resource-type) "_history"
                                " WHERE _logical_id = '" id "'"
                                " and _state <> 'deleted'"
                                " ORDER BY _last_modified_date DESC"
                                " LIMIT 1"))]
    (when version
      (update-in version [:json] clean-json))))

(defn select-latest-version-id [db-spec resource-type id]
  (let [[{vid :version-id}]
        (run-query db-spec (str "SELECT _version_id::varchar as \"version-id\""
                                " FROM fhir.view_" (.toLowerCase resource-type) "_history"
                                " WHERE _logical_id = '" id "'"
                                " and _state <> 'deleted'"
                                " ORDER BY _last_modified_date DESC"
                                " LIMIT 1"))]
    vid))

(defn select-history [db-spec resource-type id]
  (let [history
        (run-query db-spec (str "SELECT _version_id::varchar as \"version-id\","
                                " _last_modified_date::varchar as \"last-modified-date\","
                                " _state as state, _logical_id as id, json::text"
                                " FROM fhir.view_" (.toLowerCase resource-type) "_history"
                                " WHERE _logical_id = '" id "'"
                                " ORDER BY _last_modified_date DESC"))]
    (map #(update-in % [:json] clean-json) history)))

(defn delete [db-spec resource-id]
  (run-query db-spec (str "SELECT fhir.delete_resource('" resource-id "')")))

(defn update [db-spec resource-id resource]
  (run-query db-spec (str "SELECT fhir.update_resource('"
                        resource-id
                        "','"
                        (json-to-string resource)
                        "'::json)::varchar")))

(defn deleted? [db-spec resource-id]
  (-> (run-query db-spec (str "SELECT count(*) FROM fhir.resource"
                           " WHERE _logical_id = '" resource-id "'"
                           " AND _state = 'deleted'"))
      first :count zero? not))

(defn exists? [db-spec resource-id]
  (let [count (:count
                (first
                  (run-query db-spec
                             (str "SELECT count(*) FROM fhir.resource"
                                  " WHERE _logical_id = '" resource-id "'"
                                  " AND _state = 'current'"))))]
    (not (zero? count))))
