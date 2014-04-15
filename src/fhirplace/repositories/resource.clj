(ns fhirplace.repositories.resource
  (:require [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]
            [fhirplace.repositories.utils :as u]
            [honeysql.core :as honey]
            [honeysql.helpers :as h])
  (:refer-clojure :exclude (delete cast)))

(defn resource-table-name [res-type]
  (str "fhir." (.toLowerCase res-type)))

(defn insert [db-spec resource]
  (u/proc-call
    db-spec
    :fhir.insert_resource
    [(u/json-to-string resource) :json]))

(defn resource-types [db-spec]
  (->> ["SELECT DISTINCT(path[1]) FROM meta.resource_elements"]
       (sql/query db-spec)
       (map :path)
       set))

(defn scope-with-logical-id [exp id]
  (h/merge-where
    exp
    [:= (u/column :_logical_id :varchar) (str id)]))

(defn scope-with-state [exp state]
  (h/merge-where
    exp
    [:= (u/column :_state :varchar) state]) )

(defn scope-without-state [exp state]
  (h/merge-where
    exp
    [:not= (u/column :_state :varchar) state]))

(defn version-scope [resource-type id]
  (-> (h/select [(u/column :_version_id :varchar) "version-id"]
                [(u/column :_last_modified_date :varchar) "last-modified-date"]
                [:_state "state"]
                [:_logical_id "id"])
      (h/from (keyword (resource-table-name resource-type)))
      (scope-with-logical-id id)
      (scope-without-state "deleted")
      (h/order-by [:_last_modified_date :desc])))

(defn scope-with-data [exp]
  (h/merge-select exp (u/column :data :text)))

(defn scope-with-version-id [exp vid]
  (h/merge-where
    exp [:= (u/column :_version_id :varchar) vid]))

(defn scope-since [exp snc]
  (u/merge-where-if exp snc [:>= :_last_modified_date snc]))

(defn scope-limit [exp limit]
  (h/limit exp limit))

(defn honey-query [db-spec query]
  (sql/query db-spec (honey/format query)))

(defn query-> [query db-spec]
  (honey-query db-spec query))

(defn fix-json [resource]
  (when resource
    (update-in resource [:data] u/clean-json)) )

(defn fix-all-json [xs]
  (map fix-json xs))

(defn select-version
  "select resource version"
  [db-spec resource-type id vid]

  (-> (version-scope resource-type id)
      (scope-with-data)
      (scope-with-version-id vid)
      (scope-limit 1)

      (query-> db-spec)
      (first)
      (fix-json)))

(defn select-latest-version
  "select latest version"
  [db-spec resource-type id]

  (-> (version-scope resource-type id)
      (scope-with-data)
      (scope-limit 1)

      (query-> db-spec)
      (first)
      (fix-json)))

(defn select-latest-version-id
  "return latest version id"
  [db-spec resource-type id]

  (-> (version-scope resource-type id)
      (scope-limit 1)

      (query-> db-spec)
      (first)
      :version_id))

(defn select-history
  "select whole history"
  [db-spec resource-type id cnt snc]

  (-> (version-scope resource-type id)
      (scope-with-data)
      (scope-limit cnt)
      (scope-since snc)

      (query-> db-spec)
      (fix-all-json)))

(defn delete
  [db-spec resource-id]

  (-> db-spec
      (u/proc-call
        :fhir.delete_resource [resource-id :uuid])))

(defn update [db-spec resource-id resource]
  (-> db-spec
      (u/proc-call
        :fhir.update_resource
        [resource-id :uuid]
        [(u/json-to-string resource) :json])))

(defn deleted? [db-spec resource-id]
  (-> (h/select :%count.*)
      (h/from  :fhir.resource)
      (scope-with-logical-id resource-id)
      (scope-with-state "deleted")
      (query-> db-spec)
      first :count zero?  not))

;; TODO: looks like here is wrong logic
;; this query do not guaranty existance
(defn exists? [db-spec resource-id]
  (-> (h/select :%count.*)
      (h/from  :fhir.resource)
      (scope-with-logical-id resource-id)
      (scope-with-state "current")
      (query-> db-spec)
      first :count zero?  not))
