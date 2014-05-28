(ns fhirplace.resources.profiles
  (:require [clojure.java.io :as cji]
            [fhir :as f]
            [saxon :as s]))

(defn- load-xml [nm]
  (-> (str "fhir/" nm)
      (cji/resource)
      (.getPath)
      (slurp)
      (s/compile-xml)))

(def prof-dom (load-xml "profiles-resources.xml"))

(def nss
  {:a "http://www.w3.org/2005/Atom"
   :f "http://hl7.org/fhir" })

(def conformance-xml
  (-> (s/query "a:feed/a:entry/a:content/f:Conformance" nss prof-dom)
      (first)
      (.toString)))

(def conf (f/from-xml conformance-xml))

(defn profile [tp]
  conf)
