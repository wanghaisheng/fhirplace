(ns fhirplace.repositories.resource
  (:require [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]
            [fhirplace.util :as util]
            [honeysql.core :as honey]
            [honeysql.helpers :as h])
  (:refer-clojure :exclude (delete)))

(defn- json-to-string [json-or-string]
  (if (string? json-or-string)
    json-or-string
    (json/write-str json-or-string)))

(defn- run-query [db-spec sql-text]
  (sql/query db-spec sql-text))

(defn clean-json [json]
  (-> json
      (json/read-str :key-fn keyword)
      util/discard-indexes
      util/discard-nils))

(defn insert [db-spec resource]
  (:insert_resource
   (first
    (run-query db-spec [(str "SELECT fhir.insert_resource('"
                             (json-to-string resource)
                             "'::json)::varchar")]))))

(defn resource-types [db-spec]
  (set
   (map :path
        (run-query db-spec ["SELECT DISTINCT(path[1]) FROM meta.resource_elements"]))))

(defn resource-table-name [res-type]
  (str "fhir." (.toLowerCase res-type)))

(defn select-version [db-spec resource-type id vid]
  (let [[version]
        (run-query db-spec
                   [(str "SELECT _last_modified_date as \"last-modified-date\","
                         " data::text"
                         " FROM " (resource-table-name resource-type)
                         " WHERE _logical_id = '" id "' and _version_id = '" vid "'"
                         " and _state <> 'deleted'"
                         " LIMIT 1")])]

    (when version
      (update-in version [:data] clean-json))))

(defn select-latest-version [db-spec resource-type id]
  (let [[version]
        (run-query db-spec
                   [(str "SELECT _version_id::varchar as \"version-id\","
                         " _last_modified_date::varchar as \"last-modified-date\","
                         " data::text"
                         " FROM " (resource-table-name resource-type)
                         " WHERE _logical_id = '" id "'"
                         " and _state <> 'deleted'"
                         " ORDER BY _last_modified_date DESC"
                         " LIMIT 1")])]
    (when version
      (update-in version [:data] clean-json))))

(defn select-latest-version-id [db-spec resource-type id]
  (let [[{vid :version-id}]
        (run-query db-spec
                   [(str "SELECT _version_id::varchar as \"version-id\""
                         " FROM " (resource-table-name resource-type)
                         " WHERE _logical_id = '" id "'"
                         " and _state <> 'deleted'"
                         " ORDER BY _last_modified_date DESC"
                         " LIMIT 1")])]
    vid))

(defn merge-where-if [expr pred where]
  (if pred
    (h/merge-where expr where)
    expr))

(defn limit-if [expr limit]
  (if limit
    (h/limit expr (Integer. limit))
    expr))

(defn ++ [& parts]
  (keyword
   (apply str
          (map name parts))))

(defn cast [column type]
  (++ column "::" type))
(cast :res :varchar)

(defn select-history-sql [resource-type id cnt snc]
  (-> (h/select [(cast :version_id :varchar) "versionsd-id"]
                [(cast :_last_modified_date :varchar) "last-modified-date"]
                [:_state "state"]
                [:_logical_id "id"]
                [(cast :data :text) "data"])
      (h/from (keyword (resource-table-name resource-type)))
      (h/where [:= (cast :_logical_id :varchar) (str id)])
      (merge-where-if snc [:>= :_last_modified_date snc])
      (limit-if cnt)
      (h/order-by [:_last_modified_date :desc])
      honey/format))
(defn select-history [db-spec resource-type id cnt snc]
  (let [history (run-query db-spec
                           (select-history-sql resource-type id cnt snc))]
    (map #(update-in % [:data] clean-json) history)))

(defn delete [db-spec resource-id]
  (run-query db-spec [(str "SELECT fhir.delete_resource('" resource-id "')")]))

(defn update [db-spec resource-id resource]
  (run-query db-spec [(str "SELECT fhir.update_resource('"
                           resource-id
                           "','"
                           (json-to-string resource)
                           "'::json)::varchar")]))

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
