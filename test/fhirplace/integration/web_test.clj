(ns fhirplace.integration.web-test
  (:use midje.sweet)
  (:require [fhirplace.system :as sys]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]
            [ring.util.request :as request]
            [ring.util.response :as response]))

(def test-system (sys/create :test))


(defn json-body [response]
  (json/read-str (:body response) :key-fn keyword))

(defn request [& args]
  ((:handler test-system) (apply mock/request args)))

(defn GET [& args]
  (apply request :get args))

(defn POST [& args]
  (apply request :post args))

(fact
  (GET "/info") =not=> nil)

(def patient (slurp "test/fixtures/patient.json"))

(facts
  "/metadata"
  (let [resp (GET "/metadata")
        conf (json-body resp)]
    resp =not=> nil
    (:status resp) => 200
    (:resourceType conf) => "Conformance"
    (get-in conf [:rest 0 :resources])=> #(< 0 (count %))
    ))

(facts
  (let [res (POST "/Patient" patient)
        location  (response/get-header res "Location")
        res-2 (GET location)]

    location =not=> nil
    (:status res) => 201
    (:body res-2) =not=> nil
    (:status res-2) => 200))
#_(
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
                res (app req-put)
                resource (parse-body (perform-request :get patient-url))]
            (:status res) => 200
            (get resource "deceasedBoolean") => true
            (clear-resources db-spec))))
