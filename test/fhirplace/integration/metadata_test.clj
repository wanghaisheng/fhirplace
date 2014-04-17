(ns fhirplace.integration.metadata-test
  (:use midje.sweet)
  (:require [ring.util.response :as response]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.test :refer :all]
            [plumbing.graph :as graph ]
            [schema.core :as s]
            [fhirplace.test-helper :refer :all]))

(use 'plumbing.core)

(def re-xml  #"\<\?xml version='1\.0' encoding='UTF-8'\?\>")
(def not-empty? (complement empty?))

(def-test-cases meta-case
  {:meta      (fnk []
                   (GET "/metadata" {:_format "application/json"}))
   :meta-json (fnk [meta]
                   (json-body meta))
   :meta-xml  (fnk []
                   (GET "/metadata" {:_format "application/xml"}))})

(deffacts "About /metadata"
  (def res (meta-case {}))

  (fact
    "metadata"
    (:meta res) => (status? 200)

    (:meta-json res)
    => (every-checker
         (contains {:resourceType "Conformance" })
         #(not-empty? (get-in % [:rest 0 :resources]))))

  (fact
    "when requesting with mime-type=application/xml
    returns resource as XML"

    (:meta-xml res) => (contains {:body re-xml})))

