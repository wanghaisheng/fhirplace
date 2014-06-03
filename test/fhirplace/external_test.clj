(ns fhirplace.external-test
  (:use midje.sweet)
  (:require [fhirplace.core :as fc]
            [fhir :as f]
            [clojure.test :refer :all]
            [plumbing.core :refer [fnk]]
            [plumbing.graph :as pg]
            [clj-http.client :as cc]))

(import 'org.hl7.fhir.instance.model.Conformance)
(import 'org.hl7.fhir.instance.model.AtomFeed)

(defmacro def-scenario  [nm m]
  `(def ~nm  (pg/lazy-compile ~m)))

(def base-url  "http://localhost:3000")
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
