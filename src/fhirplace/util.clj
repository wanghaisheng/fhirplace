(ns fhirplace.util
  (:require
   [clojure.java.io :as io]))

(import 'java.io.File)

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
  ([{:keys [host protocol port]} resource-type id]
    (-> (str protocol "://" host ":" port)
        (str "/" resource-type "/" id)))
  ([system resource-type id vid]
    (-> (cons-url system resource-type id)
        (str "/_history/" vid))))

(defn resource-to-file
  "Makes instance of File class for something
   located in `resources' directory. Mainly
   used in schematron validations and XSLT processing."
  [path] {:pre [(not (nil? (io/resource path)))]}
  (-> (io/resource path)
    (.toURI)
    (File.)))
