(ns fhirplace.util
  (:require [clj-time.format :as time]
            [clj-time.coerce :as time-coerce]
            [clojure.java.io :as io])
  (:import java.io.File))

(defn discard-nils [m]
  (reduce (fn [acc [k v]]
            (cond
              (map? v) (assoc acc k (discard-nils v))
              (sequential? v) (assoc acc k
                                     (mapv (fn [x]
                                             (if (coll? x)
                                               (discard-nils x)
                                               x))
                                           v))
              (nil? v) acc
              :else (assoc acc k v)))
          {} m))

(defn discard-indexes [m]
  (reduce (fn [acc [k v]]
            (cond
              (map? v) (assoc acc k (discard-indexes v))
              (sequential? v) (assoc acc k
                                     (mapv (fn [x]
                                             (if (coll? x)
                                               (discard-indexes x)
                                               x))
                                           v))
              (= :_index k) acc
              (= :_version_id k) acc
              :else (assoc acc k v)))
          {} m))

(defn cons-url
  ([system resource-type id]
     (str (cons-url system)
          "/" resource-type "/" id))
  ([system resource-type id vid]
     (str (cons-url system resource-type id)
          "/_history/" vid))
  ([{:keys [host protocol port]}]
     (str protocol "://" host ":" port)))

(defn resource-to-file
  "Makes instance of File class for something
   located in `resources' directory. Mainly
   used in schematron validations and XSLT processing."
  [path] {:pre [(not (nil? (io/resource path)))]}
  (-> (io/resource path)
    (.toURI)
    (File.)))

(defn format-json? [fmt]
  (or (not fmt)
      (#{"json"
         "application/json"
         "application/json+fhir"} fmt)))

(defn format-xml? [fmt]
  (#{"xml"
     "text/xml"
     "application/xml"
     "application/xml+fhir"} fmt))

(defn from-sql-time-string [string]
  (when string
    (let [formatter (time/formatter "YYYY-MM-dd HH:mm:ss.SSSSSSZZ")]
      (time/parse formatter string))))

(defn to-sql-time [time]
  (when time
    (time-coerce/to-sql-time time)))

