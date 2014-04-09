(ns fhirplace.resources.validation
  (:require
    [clojure.string :as string]
    [fhirplace.resources.xsd :as xsd]
    [saxon :as xml]
    [fhirplace.resources.schematron :as sch]))

(def ^{:private true} xsd-validator
  (delay (xsd/mk-validator "fhir/fhir-all.xsd")))

(def ^{:private true} schematrons
  (atom {}))

(defn- load-schema [res-type]
  (sch/compile-sch
    (str "fhir/"
         (string/lower-case res-type)
         ".sch")))

(defn- get-schematron-validator [res-type]
  (or (get @schematrons res-type)
      (get (swap! schematrons
                  #(assoc % res-type (load-schema res-type)))
           res-type)))

(defn errors
  "validate resource and return vec of errors
  or nil"
  [xml-str]
  (let [xmldoc (xml/compile-xml xml-str)
        res-type (xml/query "local-name(/*)" xmldoc)]

    (if-let [error (@xsd-validator xmldoc)]
      [{:type "xsd" :message error}]
      ((get-schematron-validator res-type) xmldoc))))
