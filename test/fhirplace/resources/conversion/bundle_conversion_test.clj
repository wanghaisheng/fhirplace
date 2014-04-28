(ns fhirplace.resources.conversion.bundle-conversion-test
  (:use [clojure.test :only (is)])
  (:require
    [fhirplace.resources.bundle :as b]
    [fhirplace.resources.conversion :as c]
    [fhirplace.test-helper :as th]
    [clojure.data.json :as json])
  (:import [java.io File StringReader]
           [javax.xml XMLConstants]
           [javax.xml.transform.stream StreamSource]
           [javax.xml.validation SchemaFactory Validator]))

(defn json2xml [a] "")

(defn timestamp []
  (-> (java.util.Date.)
      (.getTime)
      (java.sql.Timestamp.)))

(def patient (th/fixture "patient"))

(defn valid-atom? [xml-string]
  (let [schema-file (File. "resources/json2xml/atom.xsd.xml")
        schema-factory (SchemaFactory/newInstance XMLConstants/W3C_XML_SCHEMA_NS_URI)
        schema (.newSchema schema-factory schema-file)
        validator (.newValidator schema)
        source (StreamSource. (StringReader. xml-string))]
    (try
      (.validate validator source)
      true
      (catch Exception exp
        (println exp)
        false))))

(let [entries [(merge patient {:last_modified_date (timestamp)})
               (merge patient {:last_modified_date (timestamp)
                               :state "deleted"})]
      bundle (b/build-bundle entries th/test-system)]
  (is (= true (valid-atom? (c/json->xml bundle)))))
