(ns fhirplace.validator
  (:import (org.hl7.fhir.instance.validation ValidationEngine
                                             Validator)))

;; 1. Load .setDefinitions
;; 2. create Validation Engine
;; 3. set .setSource
;; 4. volidate!!olol
;; 5. PROFIT!!!

(defn load-definitions []
  (let [files (file-seq (clojure.java.io/file "definitions"))]
    (reduce (fn [acc file]
              (assoc acc
                (.getName file)
                (.getBytes (slurp (.getAbsolutePath file)))))
            {} (rest files))))

(defn set-definitions! [validation-engine definitions]
  (doto (.getDefinitions validation-engine)
    (.putAll definitions)))

(defn set-source! [validation-engine source]
  (.setSource validation-engine (.getBytes source)))

(defn validate [xml]
  (doto (ValidationEngine.)
      (set-definitions! (load-definitions))
      (set-source! xml)
      (.process)
      (.getOutputs)))


(def xml "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Patient xmlns=\"http://hl7.org/fhir\"></Patient>")
(validate xml)
(doto (Validator.)
  (.setSource xml)
  (.setDefinitions "validation.zip")
  (.process))

(String. (.get (java.util.HashMap. (load-definitions)) "iso_svrl_for_xslt1.xsl"))










