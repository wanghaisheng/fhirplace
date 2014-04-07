(ns fhirplace.resources.conversion-test
  (:use midje.sweet)
  (:require
    [fhirplace.resources.conversion :as c]
    [fhirplace.resources.validation :as v]
    [saxon :as xml]))

(def pt-json
  (slurp "test/fixtures/patient.json"))

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
  (v/errors "Patient" pt-xml)=> nil)
