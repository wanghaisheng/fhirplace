(ns fhirplace.handler-test
  (:use midje.sweet
        fhirplace.core
        fhirplace.app
        ring.util.request
        ring.mock.request)
  (:require [fhirplace.handler :refer :all]
            [clojure.java.jdbc :as sql]
            [clojure.data.json :as json]
            [fhirplace.db :as db]
            [fhirplace.core :as core]
            [fhirplace.system :as system]
            [ring.util.response :as response])) 

(def db-spec (db/conn))

(defn read-patient []
  (slurp "test/fixtures/patient.json"))

(def app (create-web-handler (system/create)))

(defn perform-request [& request-params]
  (app (apply request request-params)))

(defn parse-body [response]
  (json/read-str (:body response)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(facts "About READ for existed resource"
  (let [patient (read-patient)
        patient-id (insert-resource db-spec patient)
        req (perform-request :get (str "/patient/" patient-id))
        res (parse-body req)]
              
      (get res "_id")          => patient-id
      (get res "resourceType") => "Patient"
      (:status req)            => 200)
    (clear-resources db-spec))

(facts "About READ for non-existed resource"
  (:status (perform-request :get "/patient/blablabla"))     => 404
  (:status (perform-request :get (str "/patient/" (uuid)))) => 404)

(facts  "About CREATE"
  (let [patient (read-patient)
        patient-json (json/read-str patient)
        req (body (request :post "/patient") patient)
        patient-url (response/get-header (app req) "Location")
        resource (parse-body (perform-request :get patient-url))]
    (get resource "resourceType") => "Patient"
    (clear-resources db-spec)))

(facts "About DELETE for existed resource"
  (let [patient (read-patient)
        patient-id (insert-resource db-spec patient)
        req (perform-request :delete (str "/patient/" patient-id))
        req-get (perform-request :get (str "/patient/" patient-id))]
      (:status req) => 204
      (:status req-get) => 404)
    (clear-resources db-spec))

(facts  "About UPDATE"
  (let [patient (read-patient)
        patient-json (json/read-str patient)
        req (body (request :post "/patient") patient)
        patient-url (response/get-header (app req) "Location")
        patient-put-json (assoc patient-json "deceasedBoolean" true)
        patient-put (json/write-str patient-put-json)
        req-put (body (request :put patient-url) patient-put)
        resource (parse-body (perform-request :get patient-url))]
    patient-url => "vasia"
    (get resource "deceasedBoolean") => true
    (comment clear-resources db-spec)))

