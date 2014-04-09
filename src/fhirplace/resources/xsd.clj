(ns fhirplace.resources.xsd
  (:require
    [clojure.java.io :as io]))

(import 'javax.xml.XMLConstants)
(import 'org.xml.sax.SAXException)
(import 'javax.xml.validation.SchemaFactory)
(import 'java.io.File)
(import 'java.io.StringReader)
(import 'javax.xml.transform.stream.StreamSource)

(defn mk-validator
  "create validation fn [xml-str]
   which return nil if all is ok and error message else
   schema - path to xsd schema"
  [schema]
  (let [schema-factory (SchemaFactory/newInstance XMLConstants/W3C_XML_SCHEMA_NS_URI)
        schema-src     (StreamSource. (File. (.getPath (io/resource schema))))
        schema         (.newSchema schema-factory schema-src)
        validator      (.newValidator schema)]
    (fn [xmldoc]
      (try
        (println "validating" xmldoc)
        (->> (StringReader. (if (string? xmldoc)
                              xmldoc
                              (.toString xmldoc)))
             (StreamSource.)
             (.validate validator))
        nil
        (catch SAXException e
          (.getMessage e)
          (str e))))))
