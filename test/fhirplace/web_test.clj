(ns fhirplace.web-test
  (:use midje.sweet)
  (:require [ring.util.request :as request]
            [ring.util.response :as response]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.data.json :as json]
            [fhirplace.web :as web]
            [fhirplace.test-helper :refer :all]))


(def patient-json (fixture "patient"))

(facts
  "wrap-with-format"
  (def chain (web/wrap-with-format (fn [x] {:body "ok"})))
  (:status (chain {:params {:_format "ups"}})) => 500
  (:body (chain {:params {:_format "application/json"}})) => "ok")


(facts
  "wrap-with-response"
  (def chain (web/wrap-with-response-serialization (fn [x] {:body (:_body x)})))
  (:body (chain {:params {:_format "application/json"} :_body "ok"})) => "ok"
  (:body (chain {:format :json :_body {:id "123-abc"}})) => "{\"id\":\"123-abc\"}"
  (:body (chain {:format :xml :_body patient-json})) => #"<Patient")
