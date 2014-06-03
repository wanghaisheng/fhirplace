(ns fhirplace.db
  (:require [clojure.java.jdbc :as cjj]
            [honeysql.core :as hc]
            [clojure.data.json :as json]
            [fhir :as f]
            [honeysql.helpers :as hh]
            [environ.core :as env]))

(import ' org.postgresql.util.PGobject)

(defn db []
  {:subprotocol (or (env/env :fhirplace-subprotocol) "postgresql")
         :subname (or (env/env :fhirplace-subname) "//127.0.0.1:5455/fhirplace")
         :user (or (env/env :fhirplace-user) "fhir")
         :stringtype (or (env/env :fhirplace-stringtype) "unspecified")
         :password (or (env/env :fhirplace-password) "fhir")})

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

(defn q [hsql]
  (let [sql (hc/format hsql)]
    (println "SQL:" sql)
    (cjj/query (db) sql)))

(defn q-one [hsql]
  (first (q hsql)))

(defn e [sql]
  (let [sql sql]
    (println "SQL:" sql)
    (cjj/execute! (db) sql)))

(defn i [tbl attrs]
  (first
    (cjj/insert! (db) tbl attrs)))

(import 'java.sql.Timestamp)

(defn -create [tp json]
  (i (tbl-name tp)
     {:logical_id (uuid)
      :version_id (uuid)
      :published (Timestamp. (System/currentTimeMillis))
      :resource_type tp
      :data json }))


(defn- move-to-history [tp id]
  (let [tbl  (tbl-name tp)
        h-tbl (htbl-name tp)]
    (e [(str "INSERT INTO " (name h-tbl) " (SELECT * FROM " (name tbl) " WHERE logical_id =? )") id])
    (e [(str "DELETE FROM " (name tbl) " WHERE logical_id =?") id])))

(defn -update [tp id json]
  (move-to-history tp id)
  (i (tbl-name tp)
     {:logical_id id
      :version_id (uuid)
      ;;TODO fix published id
      :published (Timestamp. (System/currentTimeMillis))
      :resource_type tp
      :data json }))

(defn -delete [tp id]
  (move-to-history tp id))

(defn- find-by-id [tp id]
  (q-one {:select [:*]
          :from [(tbl-name tp)]
          :where [:and
                  [:= :resource_type tp]
                  [:= :logical_id id]]
          :limit 1}))

(defn- find-hist-by-id [tp id vid]
  (or
    (q-one {:select [:*]
            :from [(tbl-name tp)]
            :where [:and
                    [:= :resource_type tp]
                    [:= :logical_id id]
                    [:= :version_id vid] ]
            :limit 1})
    (q-one {:select [:*]
            :from [(htbl-name tp)]
            :where [:and
                    [:= :resource_type tp]
                    [:= :logical_id id]
                    [:= :version_id vid]]
            :limit 1})))


(defn -read [tp id]
  (find-by-id tp id))

(defn -vread [tp id vid]
  (find-hist-by-id tp id vid))

(defn exists? [id]
  (->
    (q {:select [:logical_id]
        :from   [:resources]
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
                  (q {:select [:*] :from [(tbl-name tp)]
                      :where [:= :resource_type tp]})))

(defn -history [tp id]
  (wrap-in-bundle  "History"
                  (into
                    (q {:select [:*] :from [(htbl-name tp)]
                        :where [:and
                                [:= :resource_type tp]
                                [:= :logical_id id]]})
                    (q {:select [:*] :from [(tbl-name tp)]
                        :where [:and
                                [:= :resource_type tp]
                                [:= :logical_id id]]}))))

#_(-history "Patient" (uuid))
