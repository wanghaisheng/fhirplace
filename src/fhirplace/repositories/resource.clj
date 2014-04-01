(ns fhirplace.repositories.resource
  (:require [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]
            [fhirplace.util :as util])
  (:refer-clojure :exclude (delete)))

(defn resource-types [db-spec]
  (set
    (map :path
      (sql/query db-spec ["SELECT DISTINCT(path[1]) FROM meta.resource_elements"]))))

(defn- json-to-string [json-or-string]
  (if (string? json-or-string)
    json-or-string
    (json/write-str json-or-string)))

(defn insert [db-spec resource]
  (:insert_resource
   (first
     (sql/query db-spec [(str "SELECT fhir.insert_resource('"
                           (json-to-string resource)
                           "'::json)::varchar")]))))

(defn clears [db-spec]
  (sql/execute! db-spec ["DELETE FROM fhir.resource"]))

(defn select [db-spec resource-type id]
  (if-let [json-str (-> (sql/query db-spec [(str "SELECT json::text"
                                           " FROM fhir.view_" (.toLowerCase resource-type)
                                           " WHERE _logical_id = '" id "'"
                                           " LIMIT 1")])
                   first
                   :json)]

    (-> json-str
        (json/read-str :key-fn keyword)
        util/discard-indexes
        util/discard-nils)))

(defn select-version [db-spec resource-type id vid]
  (if-let [json-str (-> (sql/query db-spec [(str "SELECT json::text"
                                           " FROM fhir.view_" (.toLowerCase resource-type) "_history"
                                           " WHERE _logical_id = '" id "' and _version_id = '" vid "'"
                                           " LIMIT 1")])
                   first
                   :json)]

    (-> json-str
        (json/read-str :key-fn keyword)
        util/discard-indexes
        util/discard-nils)))

(defn select-history [db-spec resource-type id]
  (let [versions (sql/query db-spec [(str "SELECT _version_id::varchar"
                                           " FROM fhir.view_" (.toLowerCase resource-type) "_history"
                                           " WHERE _logical_id = '" id "'"
                                           " ORDER BY _last_modified_date DESC")])]

    (map :_version_id versions)))

(defn delete [db-spec resource-id]
  (sql/execute! db-spec [(str "DELETE FROM fhir.resource WHERE _logical_id = '" resource-id "'")]))

(defn update [db-spec resource-id resource]
  (sql/query db-spec [(str "SELECT fhir.update_resource('"
                        resource-id
                        "','"
                        (json-to-string resource)
                        "'::json)::varchar")]))

(defn exists? [db-spec resource-id]
  (let [count (:count 
                (first 
                  (sql/query db-spec 
                             [(str "SELECT count(*) from fhir.resource where _logical_id = '"
                                   resource-id "'")])))]
    (not (zero? count))))

