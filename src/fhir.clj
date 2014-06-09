(ns fhir
  (:require
    [fhir.conv :as fc]
    [fhir.validation :as fv]
    [cheshire.core :as cc]
    [fhir.bundle :as fb]
    [fhir.profiles :as fp]))

(def re-xml #"(?m)^<.*>")
(def re-json #"(?m)^[{].*")

(defn parse [x]
  "parse xml or json string
  throw error"
  (cond
    (re-seq re-xml x) (fc/from-xml x)
    (re-seq re-json x) (fc/from-json x)
    :else (throw (Exception. "Don't know how to parse: " (pr-str x)))))

(defn serialize [fmt x]
  (cond
    (= fmt :xml) (fc/to-xml x)
    (= fmt :json) (fc/to-json x)))

(defn errors [x]
  (fv/errors x))

(defn conformance []
  fp/conformance)

(defn profile [res-type]
  "return clojure representation of resource profile"
  (cc/parse-string
    (serialize :json
               (.getResource
                 (fp/profile res-type)))
    true))

(defn profile-resource [res-type]
  "return fhir representation of resource profile"
  (fp/profile-resource res-type))

(defn bundle [attrs]
  "build bundle from hash-map
  with entry :content parsed to fhir.model.Resource"
  (fb/bundle attrs))
