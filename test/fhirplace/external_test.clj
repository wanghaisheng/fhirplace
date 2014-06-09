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
(import 'org.hl7.fhir.instance.model.Patient)
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

(defn DELETE [url]
  (println "DELETE: " url)
  (cc/delete url (merge {:throw-exceptions false})))

(defn get-header
  [h res]
  (get-in res [:headers h]))

(defn mime-type
  [fmt]
  ({"xml" "application/xml+fhir"
    "json" "application/json+fhir"} fmt))

(def-scenario simple-crud
  {:metadata (fnk [] (GET (url "metadata")))
   :conformance (fnk [metadata] (f/parse (:body metadata)))

   :search (fnk [] (GET (url "Patient" "_search")))
   :search_atom (fnk [search] (f/parse (:body search)))

   :new_resource (fnk [] (POST
                           (url "Patient")
                           {:body (fixture "patient.json")}))
   :new_resource_loc (fnk [new_resource] (get-header "Content-Location" new_resource))

   :get_new_resource (fnk [new_resource_loc] (GET (url new_resource_loc)))

   :update_resource  (fnk [new_resource_loc]
                          (let [[rt id vid] (cs/split new_resource_loc #"/")]
                            (PUT (url rt id)
                                 {:headers {"Content-Location" new_resource_loc}
                                  :body    (fixture "patient.json")})))

   :updated_version  (fnk [update_resource]
                          (GET (url (get-header "Content-Location" update_resource))))


   :history_of_resource  (fnk [update_resource]
                              (let [[rt id vid] (cs/split (get-header "Content-Location" update_resource) #"/")]
                                (GET (url rt id "_history"))))

   :delete_resource  (fnk [new_resource_loc]
                          (let [[rt id vid] (cs/split new_resource_loc #"/")]
                            (DELETE (url rt id))))
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

  (status? 200 (:update_resource subj))

  (status? 200 (:updated_version subj))

  (is (instance? Patient
                 (f/parse (:body (:updated_version subj)))))


  (status? 200 (:history_of_resource subj))

  (is (instance? AtomFeed
                 (f/parse (:body (:history_of_resource subj)))))

  (status? 204 (:delete_resource subj))
  )

(run-tests)


(comment
  (def-scenario create-interaction
    {
     :create-resource (fnk [resource-type format] (POST (url (str resource-type "?_format=" (mime-type format))) {:body (fixture (str (cs/lower-case resource-type) "." format))}))
     :create-location (fnk [create-resource] (get-header "Location" create-resource))
     :create-location-parts (fnk [create-location] (re-matches #"(.*)/(.*)/(.*)/_history/(.*)" create-location))
     :create-location-base (fnk [create-location-parts] (create-location-parts 1))
     :create-location-type (fnk [create-location-parts] (create-location-parts 2))
     :create-location-id (fnk [create-location-parts] (create-location-parts 3))
     :create-location-vid (fnk [create-location-parts] (create-location-parts 4))
     })

  (def-scenario create-bad-interaction
    {
     :create-resource-bad (fnk [resource-type format] (POST (url (str resource-type "?_format=" (mime-type format))) {:body "can not parse this body"}))
     :create-resource-bad-body (fnk [create-resource-bad] (f/parse (:body create-resource-bad)))
     })

  (def-scenario create-not-interaction
    {
     :create-resource-not (fnk [resource-type format] (POST (url (str "NotFound" resource-type "NotFound" "?_format=" (mime-type format))) {}))
     })

  (def-scenario create-unprocessable-interaction
    {
     :create-resource-unprocessable (fnk [resource-type format] (POST (url (str resource-type "?_format=" (mime-type format))) {:body (fixture (str "invalid-" (cs/lower-case resource-type) "." format))}))
     :create-resource-unprocessable-body (fnk [create-resource-unprocessable] (f/parse (:body create-resource-unprocessable)))
     })

  ((deftest test-create-interaction
     (doseq [fmt ["json" "xml"] res ["Alert" "Observation" "Patient"]]
       (let [create-subject (create-interaction {:resource-type res :format fmt})
             create-bad-subject (create-bad-interaction {:resource-type res :format fmt})
             create-not-subject (create-not-interaction {:resource-type res :format fmt})
             create-unprocessable-subject (create-unprocessable-interaction {:resource-type res :format fmt})]
         (status? 201 (:create-resource create-subject))
         (is (= (:create-location-base create-subject) base-url))
         (is (= (:create-location-type create-subject) res))
         (is (:create-location-id create-subject))
         (is (:create-location-vid create-subject))

         (status? 400 (:create-resource-bad create-bad-subject))
         (is (instance? OperationOutcome (:create-resource-bad-body create-bad-subject)))

         (status? 404 (:create-resource-not create-not-subject))

         (status? 422 (:create-resource-unprocessable create-unprocessable-subject))
         (is (instance? OperationOutcome (:create-resource-unprocessable-body create-unprocessable-subject)))))))


  )
