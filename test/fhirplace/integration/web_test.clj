(ns fhirplace.integration.web-test
  (:use midje.sweet)
  (:require [fhirplace.system :as sys]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]
            [ring.util.request :as request]
            [ring.util.response :as response]
            [clojure.test :refer :all]
            [clojure.string :as str]))

(def test-system (sys/create :test))

(defn make-uuid [] (str (java.util.UUID/randomUUID)))

(defn json-body [response]
  (json/read-str (:body response) :key-fn keyword))

(defn request [& args]
  ((:handler test-system) (apply mock/request args)))

(defn GET [& args]
  (apply request :get args))

(defn POST [& args]
  (apply request :post args))

(def patient-json-str (slurp "test/fixtures/patient.json"))
(def patient-json (json/read-str patient-json-str :key-fn keyword))

(defmacro deffacts [str & body]
  (let [smbl (symbol (str/replace str #"[^a-zA-Z]" "_"))]
    `(deftest ~smbl
       (facts ~str
         ~@body))))

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
      (:address (json-body read-response)) => (:address patient-json)
      (:status read-response) => 200)))

#_(deffacts "About READ for existed resource"
  (let [patient-id (insert-resource db-spec patient-json-str)
        req (perform-request :get (str "/patient/" patient-id))
        res (parse-body req)]

    (get res "_id")          => patient-id
    (get res "resourceType") => "Patient"
    (:status req)            => 200)
  (clear-resources db-spec))

#_(
   (facts "About READ for non-existed resource"
          (:status (perform-request :get "/patient/blablabla"))     => 404
          (:status (perform-request :get (str "/patient/" (uuid)))) => 404)

   (facts "About DELETE for existed resource"
          (let [patient-json-str (read-patient)
                patient-id (insert-resource db-spec patient-json-str)
                req (perform-request :delete (str "/patient/" patient-id))
                req-get (perform-request :get (str "/patient/" patient-id))]
            (:status req) => 204
            (:status req-get) => 404)
          (clear-resources db-spec))

   (facts  "About UPDATE"
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
