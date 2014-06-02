(ns fhirplace.external-test
  (:use midje.sweet)
  (:require [fhirplace.core :as fc]
            [fhir :as f]
            [clj-http.client :as cc]))

(fact
  "/metadata"
  (let [resp (cc/get "http://localhost:3000/metadata")]
    (:status resp) => 200

    (f/parse f)
    ))

#_(cc/post "http://localhost:3000/Encounter?_format=application/json")

#_(fact
    (cc/post "http://localhost:3000/Encounter?_format=application/json")
    )
