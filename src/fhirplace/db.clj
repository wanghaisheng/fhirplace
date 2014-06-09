(ns fhirplace.db
  (:require [clojure.java.jdbc :as cjj]
            [honeysql.core :as hc]
            [clojure.data.json :as json]
            [fhir :as f]
            [honeysql.helpers :as hh]
            [environ.core :as env]))

(import ' org.postgresql.util.PGobject)

(def db
  {:subprotocol (env/env :fhirplace-subprotocol)
   :subname (env/env :fhirplace-subname)
   :user (env/env :fhirplace-user)
   :stringtype "unspecified"
   :password (env/env :fhirplace-password)})

(defn uuid  [] (java.util.UUID/randomUUID))

(defn- tbl-name [tp]
  (keyword (.toLowerCase (name tp))))

(defn- htbl-name [tp]
  (keyword (str (.toLowerCase (name tp)) "_history")))

(extend-protocol cjj/IResultSetReadColumn
  PGobject
  (result-set-read-column  [pgobj metadata idx]
    (let  [type  (.getType pgobj)
           value  (.getValue pgobj)]
      (case type
        "jsonb" value
        :else value))))

(defn q* [sql]
  (println "SQL:" sql)
  (cjj/query db sql))

(defn q [hsql]
  (let [sql (hc/format hsql)]
    (println "SQL:" sql)
    (cjj/query db sql)))

(defn q-one [hsql]
  (first (q hsql)))

(defn e [sql]
  (let [sql sql]
    (println "SQL:" sql)
    (cjj/execute! db sql)))

(defn i [tbl attrs]
  (first
    (cjj/insert! db tbl attrs)))

(import 'java.sql.Timestamp)

(defn -create [tp json]
  (let [id (:insert_resource (first (q* ["SELECT insert_resource(?)" json])))]
    (q-one {:select [:*]
            :from [(tbl-name tp)]
            :where [:= :logical_id id]})))

(defn -update [tp id json]
  (let [id (:update_resource (first (q* ["SELECT update_resource(?, ?)" id, json]))) ]
    (q-one {:select [:*]
            :from [(tbl-name tp)]
            :where [:= :logical_id id]})))

(defn -delete [tp id]
  (:delete_resource (first (q* ["SELECT delete_resource(?, ?)" id tp]))))

(defn -deleted? [tp id]
  (and
    (not (q-one {:select [:logical_id]
                 :from [(tbl-name tp)]
                 :where [:= :logical_id id]}))
    (q-one {:select [:logical_id]
            :from [(htbl-name tp)]
            :where [:= :logical_id id]})))

(defn -latest? [tp id vid]
  {:pre [(not (nil? tp))]}
  (println "-latest?" tp " " id " " vid)
  (q-one {:select [:*]
          :from [(tbl-name tp)]
          :where [:and
                  [:= :logical_id id]
                  [:= :version_id vid] ]
          :limit 1}))


(defn- find-by-id [tp id]
  (q-one {:select [:*]
          :from [(tbl-name tp)]
          :where [:and
                  [:= :logical_id id]]
          :limit 1}))

(defn- find-hist-by-id [tp id vid]
  (or
    (q-one {:select [:*]
            :from [(tbl-name tp)]
            :where [:and
                    [:= :logical_id id]
                    [:= :version_id vid] ]
            :limit 1})
    (q-one {:select [:*]
            :from [(htbl-name tp)]
            :where [:and
                    [:= :logical_id id]
                    [:= :version_id vid]]
            :limit 1})))

(defn -read [tp id]
  (find-by-id tp id))

(defn -vread [tp id vid]
  (find-hist-by-id tp id vid))

(defn -resource-exists? [tp id]
  (->
    (q {:select [:logical_id]
        :from   [(tbl-name tp)]
        :where  [:= :logical_id (java.util.UUID/fromString id)]
        :limit 1})
    first
    nil?
    not))

(defn- wrap-in-bundle [title rows]
  (let [entries (mapv (fn [x]
                        {:id (:logical_id x)
                         :updated (:last_modified_date x)
                         :published (:last_modified_date x)
                         :content (f/parse (:data x))
                         }) rows) ]

    (f/bundle {:resourceType "Bundle"
               :title title
               :updated "2012-09-20T12:04:45.6787909+00:00"
               :id (uuid)
               :entry entries})))

(defn -search [tp]
  (wrap-in-bundle "Search"
                  (q {:select [:*] :from [(tbl-name tp)] })))

(defn -history [tp id]
  (wrap-in-bundle  "History"
                  (into
                    (q {:select [:*] :from [(htbl-name tp)]
                        :where [:and
                                [:= :logical_id id]]})
                    (q {:select [:*] :from [(tbl-name tp)]
                        :where [:and
                                [:= :logical_id id]]}))))

#_(-history "Patient" (uuid))
