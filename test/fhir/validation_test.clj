(ns fhir.validation-test
  (:use midje.sweet)
  (:require
    [fhir.conv :as fc]
    [fhir.validation :as fv]))


(defmacro let-fact [doc-str lets & body]
  `(fact ~doc-str
         (let [~@lets] ~@body)))

(defn slurp-res [pth]
  (fc/from-xml (slurp pth)))

(slurp-res "test/fixtures/patient.xml")

(let-fact
  "valid"
  [pt (slurp-res "test/fixtures/patient.xml")]
  (fv/errors pt) => nil )

#_(let-fact
  "xsd errors"
  [inv-pt (slurp-res "test/fixtures/invalid-patient.xml")
   errors (fv/errors inv-pt)]

  errors =not=> empty?
  errors => (one-of map?)
  (first errors) => (contains {:type "xsd"}))

(let-fact
  "xsd errors"
  [inv-pt (slurp-res "test/fixtures/patient-invalid-schematron.xml")
   errors (fv/errors inv-pt)]

  errors =not=> nil?
  errors => (one-of map?))


