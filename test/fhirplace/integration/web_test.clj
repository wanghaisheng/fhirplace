(ns fhirplace.integration.web-test
  (:use midje.sweet)
  (:require [ring.util.request :as request]
            [ring.util.response :as response]
            [clojure.data.json :as json]
            [fhirplace.test-helper :refer :all]))


(defn make-uuid [] (str (java.util.UUID/randomUUID)))

(defn json-body [response]
  (json/read-str (:body response) :key-fn keyword))

(def patient-json-str (fixture-str "patient"))
(def patient-json (fixture "patient"))

(deffacts "FHIRPlace respond to /info with debug info"
  (fact
    (GET "/info") =not=> nil))

(deffacts "About /metadata"
  (let [resp (GET "/metadata")
        conf (json-body resp)]

    (fact "respond with not-empty body"
      resp =not=> nil)

    (fact "respond with 200 HTTP status"
      (:status resp) => 200)

    (fact "returns Conformance resource"
      (:resourceType conf) => "Conformance")

    (fact "Conformance resource contains :rest key with all available resources"
      (get-in conf [:rest 0 :resources])=> #(< 0 (count %)))))

(deffacts "About CREATEing new resource"
  (let [create-response (POST "/Patient" patient-json-str)
        redirect-location  (response/get-header create-response "Location")
        read-response (GET redirect-location)]

    (fact "returns location of newly created resource"
      redirect-location => #"/Patient/.+")

    (fact "respond with 201 HTTP status"
      (:status create-response) => 201)

    (fact "when requesting newly created resource"
      (:body read-response) =not=> nil
      (:name (json-body read-response)) => (:name patient-json)
      (:status read-response) => 200)))

(deffacts "About READing non-existent resource"
  (let [response (GET (str "/patient/" (make-uuid)))]
    (:status response) => 404
    (:body response) => "Not Found"))

#_(
   (facts "About DELETE for existed resource"
          (let [patient-json-str (read-patient)
                patient-id (insert-resource db-spec patient-json-str)
                req (perform-request :delete (str "/patient/" patient-id))
                req-get (perform-request :get (str "/patient/" patient-id))]
            (:status req) => 204
            (:status req-get) => 404)
          (clear-resources db-spec))

   (facts "About UPDATE"
          (let [patient-json-str (read-patient)
                patient-json-str (json/read-str patient-json-str)
                req (body (request :post "/patient") patient-json-str)
                patient-url (response/get-header (app req) "Location")
                patient-put-json (assoc patient-json-str "deceasedBoolean" true)
                patient-put (json/write-str patient-put-json)
                req-put (body (request :put patient-url) patient-put)
                res (app req-put)
                resource (parse-body (perform-request :get patient-url))]
            (:status res) => 200
            (get resource "deceasedBoolean") => true
            (clear-resources db-spec))))
