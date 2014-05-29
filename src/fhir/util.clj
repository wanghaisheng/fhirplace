(ns fhir.util
  (:require [saxon :as s]
            [clojure.java.io :as io])
  (:import java.io.File))

(defn load-resource [path]
  {:pre [(not (nil? (io/resource path)))]}
  (-> (io/resource path)
      (.toURI)
      (.getPath)
      (File.)))

(defn load-xml [path]
  (-> (load-resource path)
      (s/compile-xml)))

(defn load-xslt [path]
  (-> (load-resource path)
      (s/compile-xslt)))
