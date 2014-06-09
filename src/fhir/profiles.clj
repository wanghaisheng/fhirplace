(ns fhir.profiles
  (:require [clojure.java.io :as cji]
            [fhir.conv :as f]
            [fhir.util :as fu]
            [saxon :as s]))

(def ^:private prof-dom
  (fu/load-xml "fhir/profiles-resources.xml"))

(def ^:private nss
  {:a "http://www.w3.org/2005/Atom"
   :f "http://hl7.org/fhir" })

(def conformance
  (f/from-xml
    (-> (s/query "a:feed/a:entry/a:content/f:Conformance" nss prof-dom)
        (first)
        (.toString))))

(def bundle
  (f/from-xml
    (slurp
      (.getAbsolutePath
        (fu/load-resource "fhir/profiles-resources.xml")))))


(defn profile [res-type]
  (first
    (filter
      (fn [x]
        (= (str "http://hl7.org/fhir/profile/"
                (.toLowerCase res-type))
           (.getId x)))
      (.getEntryList bundle))))

(defn profile-resource [res-type]
  (.getResource
    (first
      (filter
        (fn [x]
          (= (str "http://hl7.org/fhir/profile/"
                  (.toLowerCase res-type))
             (.toLowerCase (.getId x))))
        (.getEntryList bundle)))))

(import 'org.hl7.fhir.instance.model.Profile)

(defn profiles []
  (filter #(instance? Profile (.getResource %)) (.getEntryList bundle)))
