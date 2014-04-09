(ns fhirplace.resources.parse
  (:require [fhirplace.resources.validation :as validation]
            [fhirplace.resources.conversion :as conversion]
            [cheshire.core :as json]
            [saxon :as xml]))

(defn- safely-parse-json [raw-json]
  (try
    [(json/parse-string raw-json) []]
    (catch Exception e
      [nil ["Mailformed JSON, could not be parsed"]])))

(defn- safely-parse-xml [raw-xml]
  (try
    [(xml/compile-xml raw-xml) []]
    (catch Exception e
      [nil ["Mailformed XML, could not be parsed"]])))

(defn- validate-xml [xml]
  (let [errors (validation/errors xml)]
    (if (empty? errors)
      [xml []]
      [nil errors])))

(defn- convert-xml-to-json [xml]
  (try
    [(conversion/xml->json xml) []]
    (catch Exception e
      [nil ["Could not convert XML to JSON"]])))

(defn- convert-json-to-xml [json]
  (try
    [(xml/compile-xml (conversion/json->xml json)) []]
    (catch Exception e
      [nil [(str "Could not convert JSON to XML: " e)]])))

(defn- do-pipeline
  "Applies collection of functions to initial arg unless one of
   function returns nil as first argument or there is no remainting functions."
  [initial & fns]
  (reduce (fn [acc f]
            (if (first acc)
              (f (first acc))
              acc))
    [initial []]
    fns))

(defmulti parse-resource-with-validations
  "Function to parse FHIR resource in any format into internal Clojure
   representation. Also it performs validations. Returns vector with
   two values: parsed resource and validation errors. If resource
   could not be parsed, first element of return value is nil."
  (fn [raw-resource format] format))

;; XML => PARSE => VALIDATE => CONVERT_TO_JSON => RETURN
(defmethod parse-resource-with-validations :xml [raw-resource _]
  (do-pipeline raw-resource
    safely-parse-xml
    validate-xml
    convert-xml-to-json))

;; JSON => PARSE => CONVERT_TO_XML => VALIDATE => RETURN ORIGINAL JSON
(defmethod parse-resource-with-validations :json [raw-resource _]
  (let [original-json (safely-parse-json raw-resource)
        result (do-pipeline (first original-json)
                 convert-json-to-xml
                 validate-xml)]
    (if (first result)
      original-json
      result)))

;; default implementation just raises exception
(defmethod parse-resource-with-validations :default [_ _]
  [nil ["Resource type can be eihter :xml or :json"]])
