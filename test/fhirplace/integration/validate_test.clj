(ns fhirplace.integration.validate-test
  (:use midje.sweet)
  (:require [ring.util.response :as response]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.test :refer :all]
            [plumbing.graph :as graph ]
            [schema.core :as s]
            [fhirplace.test-helper :refer :all]))

(use 'plumbing.core)

(def-test-cases test-case
  {:pt-str      (fnk [] (fixture-str "patient"))
   :post        (fnk [pt-str] (POST "/Patient" pt-str))

   :inv-pt-str  (fnk [] (fixture-str "invalid-patient"))
   :inv-post    (fnk [inv-pt-str] (POST "/Patient" inv-pt-str))

   :bad-json    (fnk [] "hi there i'm invalid json lol")
   :bad-post    (fnk [bad-json] (POST "/Patient" bad-json))})


(deftest validation-test
  (def res (test-case {}))

  (fact
    "when validating valid resource should respond with 200"
    (:post res)
    => (every-checker
         (status? 201)
         (contains {:body ""})))

  (fact
    "when validating invalid resource should respond with 422"
    (:inv-post res)
    => (every-checker
         (status? 422)
         (body-contains [:resourceType] "OperationOutcome")))

  (fact
    "when received request with broken body should respond with 400"
    (:bad-post res)
    => (every-checker
         (status? 400)
         (body-contains [:issue 0 :details] "Resource cannot be parsed"))))
