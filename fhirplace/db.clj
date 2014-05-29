(ns fhirplace.db
  (:require [clojure.java.jdbc :as cjj]
            [honeysql.core :as hc]
            [clojure.data.json :as json]
            [honeysql.helpers :as hh]))

(import ' org.postgresql.util.PGobject)

(def db {:subprotocol "postgresql"
         :subname "//127.0.0.1:5433/fhirplace"
         :user "fhir"
         :stringtype "unspecified"
         :password "fhir"})

(defn uuid  [] (java.util.UUID/randomUUID))

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
    (cjj/query db sql)))

(defn create-resource [tp json]
  (first
    (cjj/insert! db
                 :resources
                 {:logical_id (uuid)
                  :version_id (uuid)
                  :resource_type tp
                  :data json })))

(defn find-resource [tp id]
  (first (q {:select [:*]
             :from [:resources]
             :where [:and
                     [:= :resource_type tp]
                     [:= :logical_id id]]
             :limit 1})))

(defn exists? [id]
  (->
    (q {:select [:*]
        :from   [:resources]
        :where  [:= :logical_id (java.util.UUID/fromString id)]
        :limit 1})
    first
    nil?
    not))

(defn search [tp]
  (q {:select [:*]
      :from [:resources]
      :where [:= :resource_type tp]}))

#_(search "Patient")

