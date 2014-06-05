(ns fhirplace.search-test
  (:use midje.sweet)
  (:require
    [fhir :as f]
    [clojure.walk :as cw]
    [ring.util.codec :as fuc]
    [fhirplace.search :as fs]))



(print
  (fs/params-to-jsquery
    "Patient"
    "identifier=http://acme.org/patient|2345"))



#_(fact "xpath->path"
        (fs/xpath->path "f:Patient/f:name") => "name.#"
        )

#_(doseq [[k v] (fs/params-info "Patient")]
    (println k)
    (when (:xpath v)
      (println v)))
