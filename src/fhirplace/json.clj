(ns fhirplace.json
  (:require
    [clojure.data.json :as json]
    [clojure.string :as string]))

(import 'java.sql.Timestamp)

(defn- convert [_ x]
  (cond
    (instance? java.util.UUID x) (.toString x)
    (instance? Timestamp x) (.toString x)
    :else x))

(defn generate [x]
  (json/write-str x :value-fn convert))
