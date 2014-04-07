(ns fhirplace.resources.conversion-test
  (:use midje.sweet)
  (:require
    [fhirplace.resources.conversion :as c]
    [fhirplace.resources.validation :as v]
    [fhirplace.test-helper :as th]
    [clojure.data.json :as json]
    [saxon :as xml]))

(def pt-json
  (th/fixture-str "patient"))

(def pt-data
  (th/fixture "patient"))

(def pt-xml
  (c/json->xml pt-json))

(spit "tmp/res.xml"  pt-xml)

(def pt-dom
  (xml/compile-xml pt-xml))

(comment (fact
  (c/get-real-key
    {:deceasedBoolean false}
    (keyword "deceased[x]"))
  => :deceasedBoolean))

(fact "handling resource text (Narrative)"
      (xml/query "count(/f:Patient/node())"
                 {:f "http://hl7.org/fhir"}
                 pt-dom) =not=> 0)

(fact "handling resource text (Narrative)"
      (xml/query "/f:Patient/f:text"
                 {:f "http://hl7.org/fhir"}
                 pt-dom) =not=> nil)

(fact "handling polimorphic attrx [x]"
      (xml/query "/f:Patient/f:deceasedBoolean"
                 {:f "http://hl7.org/fhir"}
                 pt-dom) =not=> nil)
(fact
  (v/errors "Patient" pt-xml) => nil)

(facts "Conversion of XML to JSON"
  (fact "outputs valid JSON"
    (json/read-str (c/xml->json pt-xml)) =not=> nil)

  (fact "outputs same JSON as original patient.json is"
    (:address (json/read-str (c/xml->json pt-xml) :key-fn keyword)) => (:address pt-data)))
