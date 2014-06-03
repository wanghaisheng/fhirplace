(ns fhirplace.external-test
  (:use midje.sweet)
  (:require [fhirplace.core :as fc]
            [fhir :as f]
            [clojure.test :refer :all]
            [plumbing.core :refer [fnk]]
            [plumbing.graph :as pg]
            [clj-http.client :as cc]
            [environ.core :as env]
            [clojure.string :as cs]))

(import 'org.hl7.fhir.instance.model.Conformance)
(import 'org.hl7.fhir.instance.model.AtomFeed)
(import 'org.hl7.fhir.instance.model.Alert)
(import 'org.hl7.fhir.instance.model.OperationOutcome)

(defmacro def-scenario [nm m]
  `(def ~nm  (pg/lazy-compile ~m)))

(def base-url (env/env :fhirplace-test-url))

(defn url [& parts]
  (apply str base-url "/" (interpose "/" parts)))

(defn fixture [nm]
  (slurp (str "test/fhirplace/fixtures/" nm)))

(defn GET [url]
  (println "GET: " url)
  (cc/get url {:throw-exceptions false}))

(defn POST [url attrs]
  (println "POST: " url)
  (cc/post url (merge {:throw-exceptions false}  attrs)))

(defn PUT [url attrs]
  (println "PUT: " url)
  (cc/put url (merge {:throw-exceptions false}  attrs)))

(def-scenario simple-crud
  {:metadata (fnk [] (GET (url "metadata")))
   :conformance (fnk [metadata] (f/parse (:body metadata)))

   :search (fnk [] (GET (url "Patient" "_search")))
   :search_atom (fnk [search] (f/parse (:body search)))

   :new_resource (fnk [] (POST (url "Patient") {:body (fixture "patient.json")}))
   :new_resource_loc (fnk [new_resource] (get-in  new_resource [:headers "Content-Location"]))

   :get_new_resource (fnk [new_resource_loc] (GET (url new_resource_loc)))
   })

(def subj (simple-crud {}))

(defn status? [status responce]
  (is (= (:status responce) status)))

(deftest test-simple-crud
  (status? 200 (:metadata subj))

  (is (instance? Conformance (:conformance subj)))

  (status? 200 (:search subj))

  (is (instance? AtomFeed (:search_atom subj)))

  (status? 201 (:new_resource subj))

  (is (not (nil? (:new_resource_loc subj))))

  (status? 200 (:get_new_resource subj))
  )

(run-tests)

(defn mime-type
  [fmt]
  ({"xml" "application/xml+fhir"
    "json" "application/json+fhir"} fmt)
  )

(def-scenario create-interaction
  {
   :create-resource (fnk [resource-type format] (POST (url (str resource-type "?_format=" (mime-type format))) {:body (fixture (str (cs/lower-case resource-type) "." format))}))})


(def create-subject (create-interaction {:resource-type "Alert" :format "json"}))

(deftest test-create-interaction
  (status? 201 (:create-resource create-subject)))
