(ns fhirplace.resources.validation-test
  (:use midje.sweet)
  (:require [fhirplace.resources.validation :as v]))


(defmacro let-fact [doc-str lets & body]
  `(fact ~doc-str
         (let [~@lets] ~@body)))

(let-fact
  "valid"
  [pt (slurp "test/fixtures/patient.xml")]

  (v/errors pt) => nil )

(let-fact
  "xsd errors"
  [inv-pt (slurp "test/fixtures/invalid-patient.xml")
   errors (v/errors inv-pt)]

  errors =not=> empty?
  errors => (one-of map?)
  (first errors) => (contains {:type "xsd"}))

(let-fact
  "xsd errors"
  [inv-pt (slurp "test/fixtures/patient-invalid-schematron.xml")
   errors (v/errors inv-pt)]

  errors =not=> nil?
  errors => (one-of map?))


