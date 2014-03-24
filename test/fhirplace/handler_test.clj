(ns fhirplace.handler-test
  (:use midje.sweet
        fhirplace.core
        fhirplace.app
        ring.mock.request)
  (:require [fhirplace.handler :refer :all]
            [clojure.java.jdbc :as sql]
            [clojure.data.json :as json])) 

(defn read-patient []
  (slurp "test/fixtures/patient.json"))

(defn perform-request [& request-params]
  (app (apply request request-params)))

(defn parse-body [response]
  (json/read-str (:body response)))

(facts "About read-handler 200 OK"
  (let [patient (read-patient)
        patient-id (insert-patient patient)
        res (parse-body 
              (perform-request :get (str "/patient/" patient-id)))]

      (get res "_id") => patient-id
      (get res "resourceType") => "Patient")
    (clear-resources))
