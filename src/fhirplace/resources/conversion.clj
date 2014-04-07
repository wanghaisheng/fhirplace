(ns fhirplace.resources.conversion
  (:require
    [fhirplace.resources.conversion.xml2json :as xml2json]
    [fhirplace.resources.conversion.json2xml :as json2xml]
    [clojure.data.json :as json]))

(defn json->xml
  "Converts JSON representation of FHIR resource into XML."
  [json-str]
  (let [json (if (string? json-str)
               (json/read-str json-str :key-fn keyword)
               json-str)]
    (json2xml/perform json)))

(defn xml->json
  "Converts XML string with FHIR resource into JSON representation."
  [xml-str]
  (xml2json/perform xml-str))
