(ns fhirplace.integration.web-test
  (:use midje.sweet)
  (:require [ring.util.request :as request]
            [ring.util.response :as response]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [fhirplace.test-helper :refer :all]))


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
        resource-location-with-history (response/get-header create-response "Location")
        resource-location (first (clojure.string/split resource-location-with-history #"/_history/"))]

    (fact "returns location of newly created resource"
      resource-location => #"/Patient/.+")

    create-response => (contains {:status 201})

    (fact "when requesting newly created resource"
      (let [read-response (GET resource-location)]
        (response/get-header read-response "Content-Location") => #"/Patient/.+/_history/.+"
        (response/get-header read-response "Last-Modified") => #"....-..-.. .+"
        (:body read-response) =not=> nil
        (:name (json-body read-response)) => (:name patient-json)
        (:status read-response) => 200))

    (fact "when requesting newly created resource by version"
      (let [read-response (GET resource-location-with-history)]
        (:body read-response) =not=> nil
        (:name (json-body read-response)) => (:name patient-json)
        (:status read-response) => 200))

    (fact "when UPDATEing existent resource"
      (let [update-body (json/write-str
                          (update-in  patient-json [:telecom] conj
                                     {:system "phone"
                                      :value "+919191282"
                                      :use "home"} ))
            update-response (PUT resource-location update-body)
            update-location (response/get-header update-response "Location")]

        (:telecom (json-body (GET update-location))) => (contains [{:system "phone"
                                                                    :value "+919191282"
                                                                    :use "home"}])
        (:status update-response) => 200
        (:body update-response) => ""))

    (fact "when DELETEing existent resource"
      (DELETE resource-location) => #(= (:status %) 204)
      (:status (GET resource-location)) => 410)))

(deffacts "About READing non-existent resource"
  (let [response (GET (str "/patient/" (make-uuid)))]
    (:status response) => 404))

(deffacts "About UPDATEing non-existent resource"
  (let [response (PUT (str "/patient/" (make-uuid)) (json/write-str patient-json))]
    (:status response) => 405))

(deffacts "About HISTORY"
  (let [create-response (POST "/Patient" patient-json-str)
        resource-loc-with-history (response/get-header create-response "Location")
        resource-loc (string/replace resource-loc-with-history #"_history/.+" "_history")]

    (GET resource-loc) => (contains {:status 200})
    (json-body (GET resource-loc)) => (contains {:resourceType "Bundle"
                                                 :entry anything})))

