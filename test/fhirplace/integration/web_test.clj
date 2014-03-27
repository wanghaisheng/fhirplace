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

(deffacts "About basic CRUD on resources"
  (let [create-response (POST "/Patient" patient-json-str)
        resource-location (response/get-header create-response "Location")]

    (fact "returns location of newly created resource"
      resource-location => #"/Patient/.+")

    (fact "respond with 201 HTTP status"
      (:status create-response) => 201)

    (fact "when requesting newly created resource"
      (let [read-response (GET resource-location)]

        (:body read-response) =not=> nil
        (:name (json-body read-response)) => (:name patient-json)
        (:status read-response) => 200))

    (fact "when UPDATEing existent resource"
      (let [update-body (json/write-str
                          (update-in  patient-json [:telecom] conj
                            {:system "phone"
                             :value "+919191282"
                             :use "home"} ))
            update-response (PUT resource-location update-body)]

        (fact "respond with 200"
          (:status update-response) => 200)

        (fact "respond with empty body"
          (slurp (:body update-response)) => "")))

    (fact "when DELETEing existent resource"
      (let [delete-response (DELETE resource-location)
            read-response (GET resource-location)]

        (fact "respond with 204"
          (:status delete-response) => 204)

        (fact "resource was actually deleted"
          (:status read-response) => 404)))))

(deffacts "About READing non-existent resource"
  (let [response (GET (str "/patient/" (make-uuid)))]
    (:status response) => 404
    (:body response) => "Not Found"))
