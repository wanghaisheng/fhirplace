(ns fhir.validation
  (:require
    [saxon :as xml]
    [fhir.conv :as fc]
    [fhir.xsd :as fx]
    [fhir.schematron :as sch]
    [clojure.data.xml :as cljxml]))

(def ^{:private true} xsd-validator
  (delay (fx/mk-validator "fhir/fhir-all.xsd")))

(def ^{:private true} schematrons
  (atom {}))

(defn- load-schema [res-type]
  (sch/compile-sch
    (str "fhir/" (.toLowerCase res-type) ".sch")))

(defn- get-schematron-validator [res-type]
  (or (get @schematrons res-type)
      (get (swap! schematrons
                  #(assoc % res-type (load-schema res-type)))
           res-type)))

(defn errors
  "validate org.fhir.instance.resource and return vec of errors or nil"
  ([fhir]
   (let [res-xml  (fc/to-xml fhir)
         xmldoc   (xml/compile-xml res-xml)
         res-type (xml/query "local-name(/*)" xmldoc)]
     (if-let [error (@xsd-validator res-xml)]
       [{:type "xsd" :message error}]
       ((get-schematron-validator res-type) xmldoc)))))
