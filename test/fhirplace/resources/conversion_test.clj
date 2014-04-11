(ns fhirplace.resources.conversion-test
  (:use midje.sweet)
  (:require
    [fhirplace.resources.conversion :as c]
    [fhirplace.resources.validation :as v]
    [fhirplace.test-helper :as th]
    [clojure.data.json :as json]
    [clojure.data.xml :as xml2]
    [saxon :as xml]))

(def pt-json
  (th/fixture-str "patient"))

(def pt-data
  (th/fixture "patient"))

(def pt-xml
  (slurp "test/fixtures/patient.xml"))

(facts "Conversion of XML to JSON"
  (fact "outputs valid JSON"
    (json/read-str (c/xml->json pt-xml)) =not=> nil)

  (fact "outputs same JSON as original patient.json is"
    (:address (c/xml->json pt-xml)) => (:address pt-data)))

(facts "Conversion of JSON to XML"
  (fact "outputs correct XML representation"
    (:address (c/xml->json (c/json->xml pt-data))) => (:address pt-data))

  (fact "outputs valid XML representation"
    (v/errors (c/json->xml pt-data)) => nil

    (let [obs-xml (c/json->xml (json/read-str (slurp "test/fixtures/observation.json") :key-fn keyword))]
      (v/errors obs-xml)) => nil))
