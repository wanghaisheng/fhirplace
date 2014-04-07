(ns fhirplace.resources.conversion.xml2json
  (:require
   [saxon :as xslt]
   [fhirplace.util :as util]
   [clojure.java.io :as io]))

(def ^{:private true} fhir-xml2json-xsl
  (delay
    (xslt/compile-xslt
      (util/resource-to-file "xml2json/fhir-xml2json.xsl"))))

(defn perform
  "Performs XML => JSON conversion of FHIR resource"
  [xml-str]
  (.getStringValue (@fhir-xml2json-xsl (xslt/compile-xml xml-str))))
