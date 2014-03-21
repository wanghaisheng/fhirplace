(ns fhirplace.validator
  (:import org.hl7.fhir.instance.validation.ValidationEngine))

;; 1. Load .setDefinitions
;; 2. create Validation Engine
;; 3. set .setSource
;; 4. volidate!!olol
;; 5. PROFIT!!!

(defn validate-xml [xml]
  (doto (Validator.)
    (.setSource xml)
    (.setDefinitions "lib/validation.zip")
    (.process)))

(validate-xml "<xml></xml>")
