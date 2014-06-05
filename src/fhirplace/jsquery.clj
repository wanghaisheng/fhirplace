(ns fhirplace.jsquery
  "simple implementation query as data
  for postgresl jsquery"
  (:require [clojure.string :as cs]))

(defn- sur [sym x]
  (str sym x sym))

(defn- q [x]
  (sur "\"" x))

(defn- encode-path [pth]
  (let [xs (cs/split pth #"\.")]
    (cs/join "."
             (map
               (fn [x]
                 (cond
                   (#{"#" "$" "%" "*"} x) x
                   :else (q x)))
               xs))))

(defn- dispatch-jsquery [x]
  (cond
    (vector? x) (cond
                  (keyword? (first x)) :op
                  (string? (first x))  :exp)
    :else (type x)))

(defmulti jsquery dispatch-jsquery)
#_(remove-all-methods jsquery)

(defmethod jsquery
  :op
  [[op & xs]]
  (apply str (interpose (sur " " (name op))
                        (map jsquery xs))))

(defmethod jsquery
  :exp
  [[path xs]]
  (str (encode-path path) " ( " (jsquery xs) " )"))

(defmethod jsquery
  java.lang.String
  [x]
  (q x))

(defmethod jsquery
  java.lang.Long
  [x] x)

(defmethod jsquery
  nil
  [x]
  (throw (Exception. (str "Ups " x))))
