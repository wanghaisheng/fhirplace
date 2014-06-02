(ns fhirplace.app
  (:use ring.util.response
        ring.util.request)
  (:require [compojure.core :as cc]
            [compojure.route :as cr]
            [compojure.handler :as ch]
            [fhir :as f]
            [fhir.operation-outcome :as fo]
            [fhirplace.db :as db]
            [ring.adapter.jetty :as jetty]))

(import 'org.hl7.fhir.instance.model.Resource)
(import 'org.hl7.fhir.instance.model.AtomFeed)

;; TODO outcomes
;; TODO vread validate
;; TODO search

(defn url [& parts]
  (apply str (interpose "/" parts)))

(defn- determine-format
  "Determines request format (:xml or :json)."
  [{{fmt :_format} :params}]
  (or (get {"application/json" :json
            "application/xml"  :xml} fmt)
      :json))

(defn <-format [h]
  "formatting midle-ware
  expected body is instance of fhir reference impl"
  (fn [req]
    (let [{bd :body :as resp} (h req)
          fmt (determine-format req)]
      ;; TODO set right headers
      (println "Formating: " bd)
      (if (and bd (or (instance? Resource bd) (instance? AtomFeed bd)))
        (assoc resp :body (f/serialize fmt bd))
        resp))))

(defn- get-stack-trace [e]
  (let [sw (java.io.StringWriter.)]
    (.printStackTrace e (java.io.PrintWriter. sw))
    (println "ERROR: " sw)
    (str sw)))

(defn <-outcome-on-exception [h]
  (fn [req]
    (try
      (h req)
      (catch Exception e
        (println "<-outcome-on-exception")
        {:status 500
         :body (fo/operation-outcome
                 {:text {:status "generated" :div "<div></div>"}
                  :issue [{:severity "fatal"
                           :details (str "Unexpected server error " (get-stack-trace e))}]})}))))

(defn ->type-supported! [h]
  (fn [{{tp :type} :params :as req}]
    (println "->type-supported!")
    (if tp
      (h req)
      {:status 404
       :body (fo/operation-outcome
               {:text {:status "generated" :div "<div></div>"}
                :issue [{:severity "fatal"
                         :details (str "Resource type [" tp "] isn't supported")}]})})))

(defn ->resource-exists! [h]
  (fn [req]
    (println "->resource-exists!")
    (h req)))

(defn ->valid-input! [h]
  (fn [req]
    (println "->valid-input!")
    (h req)))

(defn ->check-deleted! [h]
  (fn [req]
    (println "->check-deleted!")
    (h req)))

(defn ->has-content-location! [h]
  (fn [req]
    (println "->has-content-location!")
    (h req)))

(defn ->has-latest-version! [h]
  (fn [req]
    (println "->has-latest-version!")
    (h req)))

(def uuid-regexp
  #"[0-f]{8}-([0-f]{4}-){3}[0-f]{12}")


(defn =metadata [req]
  {:body (f/conformance)})

(defn =search [{{rt :type} :params}]
  {:body (db/-search rt)})

(defn =history [{{rt :type id :id} :params}]
  {:body (db/-history rt id)})

(defn resource-resp [res]
  (-> {:body (f/parse (:data res))}
      (header "Content-Location" (url (:resource_type res) (:logical_id res) (:version_id res)))
      (header "Last-Modified" (:last_modified_date res))))

(defn =create
  [{{rt :type} :params body :body}]
  (let [body (slurp body)
        res (f/parse body)
        json (f/serialize :json res)
        item (db/-create (str (.getResourceType res)) json)]
    (-> (resource-resp item)
        (status 201))))

(defn =update
  [{{rt :type id :id} :params body :body}]
  (let [body (slurp body)
        res (f/parse body)
        json (f/serialize :json res)
        item (db/-update rt id json)]
    (-> (resource-resp item)
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

(defn =vread [{{rt :type id :id vid :vid} :params}]
  (let [res (db/-vread rt id vid)]
    (println res)
    (-> (resource-resp res)
        (status 200))))
