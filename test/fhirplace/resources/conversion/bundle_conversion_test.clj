(ns fhirplace.resources.conversion.bundle-conversion-test
  (:use [clojure.test :only (is)])
  (:require
    [fhirplace.resources.bundle :as b]
    [fhirplace.resources.conversion :as c]
    [fhirplace.resources.validation :as v]
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
        false))))

(let [entries [{:last_modified_date (timestamp)}
                      {:last_modified_date (timestamp)
                       :state "deleted"}]
      bundle (b/build-bundle entries th/test-system)]
  (is (= true (valid-atom? (json2xml bundle)))))
