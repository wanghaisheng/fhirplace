(ns fhirplace.repositories.utils
  (:require [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]
            [fhirplace.util :as util]
            [honeysql.core :as honey]
            [clojure.string :as string]
            [honeysql.helpers :as h])
  (:refer-clojure :exclude (delete cast)))

(defn json-to-string [json-or-string]
  (if (string? json-or-string)
    json-or-string
    (json/write-str json-or-string)))

(defn clean-json [json]
  (-> json
      (json/read-str :key-fn keyword)
      util/discard-indexes
      util/discard-nils))

(defn merge-where-if [expr pred where]
  (if pred
    (h/merge-where expr where)
    expr))

(defn limit-if [expr limit]
  (if limit
    (h/limit expr (Integer. limit))
    expr))

(defn- ++ [& parts]
  (keyword
    (apply str
           (map name parts))))

(defn cast [col type]
  (++ col "::" type))

(defn- sql-mk-arg [x]
  (if (vector? x)
    (str "?::" (name (second x)))
    "?"))

(defn- sql-mk-val [x]
  (if (vector? x) (first x) x))

(defn- sql-apply
  "generate sql for proc call"
  [fn-nm & args]
  (let [args* (map sql-mk-arg args)
        vals (map sql-mk-val args)]
    (into [(str "SELECT "(name fn-nm) "(" (string/join "," args*)  ")")]
          vals)))

(defn- proc-nm
  "calculate proc name, removing schema name"
  [fn-name]
  {:post [(keyword? %)]}
  (-> (name fn-name)
      (string/split #"\.")
      last
      keyword))

(defn proc-call
  "call postgresql function"
  [db-spec fn-name & apply-args]
  (let [pr-nm (proc-nm fn-name)]
    (-> (sql/query
          db-spec
          (apply sql-apply fn-name apply-args))
        first
        (get pr-nm))))

