(ns fhirplace.app
  (:use ring.util.response
        ring.util.request)
  (:require [compojure.core :as cc]
            [compojure.route :as cr]
            [compojure.handler :as ch]
            [fhir :as f]
            [fhirplace.db :as db]
            [ring.adapter.jetty :as jetty]))

;; TODO outcomes
;; TODO vread validate
;; TODO search

(def uuid-regexp
  #"[0-f]{8}-([0-f]{4}-){3}[0-f]{12}")

(defn url [& parts]
  (apply str (interpose "/" parts)))

(defn =metadata [req]
  {:body (f/serialize :json (f/conformance))})

(defn =search [{{rt :type} :params}]
  {:body (f/serialize :json (db/-search rt))})

(defn =history [{{rt :type id :id} :params}]
  {:body (f/serialize :json (db/-history rt id))})

(defn resource-resp [res]
  (-> (response (f/serialize :json (:data res)))
      (header "content-location" (url (:resource_type res) (:logical_id res) (:version_id res)))
      (header "Last-Modified" (:last_modified_date res))))

(defn =create
  [{{rt :type} :params body :body}]
  (let [body (slurp body)
        res (f/parse body)
        json (f/serialize :json res)
        item (db/-create rt json)]
    (-> (resource-resp res)
        (status 201))))

(defn =update
  [{{rt :type id :id} :params body :body}]
  (let [body (slurp body)
        res (f/parse body)
        json (f/serialize :json res)
        item (db/-update rt id json)]
    (-> (resource-resp res)
        (status 200))))

(defn =delete
  [{{rt :type id :id} :params body :body}]
  (-> (response (db/-delete rt id))
      (status 204)))

;;TODO add checks
(defn =read [{{rt :type id :id} :params}]
  (let [res (db/-read rt id)]
    (-> (resource-resp res)
        (status 200))))
