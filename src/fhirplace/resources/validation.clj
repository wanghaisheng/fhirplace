(ns fhirplace.resources.validation
  (:require
    [clojure.string :as string]
    [fhirplace.resources.xsd :as xsd]
    [fhirplace.resources.schematron :as sch]))


(def ^{:private true} xsd-schema
  (delay (xsd/mk-validator "fhir/fhir-all.xsd")))

(def ^{:private true} schematrons
  (atom {}))

(defn- load-schema [res-type]
  (println (str "fhir/"
              (string/lower-case res-type)
              ".sch"))
  (sch/compile-sch
    (str "fhir/"
         (string/lower-case res-type)
         ".sch")))


(defn- get-schema [res-type]
  (or (get @schematrons res-type)
      (get (swap! schematrons
                  #(assoc % res-type (load-schema res-type)))
           res-type)))

(defn errors
  "validate resource and return vec of errors
  or nil"
  [res-type xml]
  (if-let [error (@xsd-schema xml)]
    [{:type "xsd" :message error}]
    ((get-schema res-type) xml)))
