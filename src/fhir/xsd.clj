(ns fhir.xsd
  (:require
    [clojure.java.io :as io]
    [fhir.util :as fu]))

(import 'javax.xml.XMLConstants)
(import 'org.xml.sax.SAXException)
(import 'javax.xml.validation.SchemaFactory)
(import 'java.io.File)
(import 'java.io.StringReader)
(import 'javax.xml.transform.stream.StreamSource)

(defn mk-validator
  "create validation fn [xml-str]
  which return nil if all is ok and error message else
  schema-path - path to xsd schema"
  [schema-path]
  (let [schema-factory (SchemaFactory/newInstance XMLConstants/W3C_XML_SCHEMA_NS_URI)
        schema-src     (StreamSource. (fu/load-resource schema-path))
        schema         (.newSchema schema-factory schema-src)
        validator      (.newValidator schema)]
    (fn [xml-str]
      (try
        (->> (StringReader. xml-str)
             (StreamSource.)
             (.validate validator))
        nil
        (catch SAXException e
          (.getMessage e)
          (str e))))))
