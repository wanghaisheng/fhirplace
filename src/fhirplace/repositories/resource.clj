(ns fhirplace.repositories.resource
  (:require [clojure.java.jdbc :as sql]
            [fhirplace.repositories.utils :as u]
            [honeysql.core :as honey]
            [clojure.data.json :as json]
            [honeysql.helpers :as h])
  (:refer-clojure :exclude (delete cast)))

(defn- table-name [res-type]
  (keyword (str "fhir." (.toLowerCase res-type))))

(defmacro def-where
  "generate merge-where scope for chaining"
  [nm args & body]
  `(defn- ~nm [exp# ~@args]
     (h/merge-where exp# ~@body)))

(def-where where-logical-id [id]
  [:= (u/cast :_logical_id :varchar) (str id)])

(def-where where-state [state]
  [:= (u/cast :_state :varchar) state])

(def-where where-state-not [state]
  [:not= (u/cast :_state :varchar) state])

(def-where where-version-id [vid]
  [:= (u/cast :_version_id :varchar) vid])

(defn- with-data [exp]
  (h/merge-select exp (u/cast :data :text)))

(defn- since [exp snc]
  (u/merge-where-if exp snc [:>= :_last_modified_date snc]))

(defn- with-limit [exp limit]
  (h/limit exp limit))

(defn- base-scope [resource-type id]
  (-> (h/select [(u/cast :_version_id :varchar) "version-id"]
                [(u/cast :_last_modified_date :varchar) "last-modified-date"]
                [:_state "state"]
                [:_logical_id "id"])
      (h/from (table-name resource-type))
      (where-logical-id id)
      (where-state-not "deleted")
      (h/order-by [:_last_modified_date :desc])))

(defn- query-> [query db-spec]
  (sql/query db-spec (honey/format query)))

(defn- fix-json [resource]
  (when resource
    (update-in resource [:data] u/clean-json)) )

(defn- fix-all-json [xs]
  (map fix-json xs))

;; PUBLIC FUNCTIONS

(defn resource-types [db-spec]
  (->> ["SELECT DISTINCT(path[1]) FROM meta.resource_elements"]
       (sql/query db-spec)
       (map :path)
       set))

(defn insert [db-spec resource]
  (u/proc-call
    db-spec
    :fhir.insert_resource
    [(u/json-to-string resource) :json]))

(defn select-version
  "select resource version"
  [db-spec resource-type id vid]

  (-> (base-scope resource-type id)
      (with-data)
      (where-version-id vid)
      (h/limit 1)

      (query-> db-spec)
      (first)
      (fix-json)))

(defn select-latest-version
  "select latest version"
  [db-spec resource-type id]

  (-> (base-scope resource-type id)
      (with-data)
      (h/limit 1)

      (query-> db-spec)
      (first)
      (fix-json)))

(defn select-latest-metadata
  "return metadata of latest version"
  [db-spec resource-type id]

  (-> (base-scope resource-type id)
      (h/limit 1)

      (query-> db-spec)
      (first)))

(defn select-history
  "select whole history"
  [db-spec resource-type id cnt snc]
  (-> (base-scope resource-type id)
      (with-data)
      (h/limit cnt)
      (since snc)

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
      (where-logical-id resource-id)
      (where-state "deleted")
      (query-> db-spec)
      first :count zero?  not))

;; TODO: looks like here is wrong logic
;; this query do not guaranty existance
(defn exists? [db-spec resource-id]
  (-> (h/select :%count.*)
      (h/from  :fhir.resource)
      (where-logical-id resource-id)
      (where-state "current")
      (query-> db-spec)
      first :count zero?  not))

(defn map-res [xs]
  (map #(json/read-str (str %) :key-fn keyword)
       (map :data xs)))

;;FIXME: temporal
(defn search [db-spec res-type]
  (-> (h/select :*)
      (h/from  (table-name res-type))
      (where-state "current")
      (h/limit 10)

      (query-> db-spec)
      ))
