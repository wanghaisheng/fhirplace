(ns fhirplace.resources.conversion.xml2json
  (:require
   [saxon :as xslt]
   [fhirplace.util :as util]
   [cheshire.core :as json]
   [clojure.data.xml :as xml]))

(def ^{:private true} fhir-xml2json-xsl
  (delay
    (xslt/compile-xslt
      (util/resource-to-file "xml2json/fhir-xml2json.xsl"))))

(defn perform
  "Performs XML => JSON conversion of FHIR resource"
  [xml]

  (let [xml-str (if (string? xml) xml (xml/emit-str xml))]
    (json/parse-string
      (.getStringValue
        (@fhir-xml2json-xsl
          (xslt/compile-xml xml-str)))
      keyword)))
