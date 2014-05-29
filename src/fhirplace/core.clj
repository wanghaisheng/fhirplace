(ns fhirplace.core
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

(defn -metadata [req]
  {:body (f/serialize :json (f/conformance))})

(defn -search [{{rt :resource-type} :params}]
  {:body (f/serialize :json (db/-search rt))})

(defn -history [{{rt :resource-type id :id} :params}]
  {:body (f/serialize :json (db/-history rt id))})

(defn resource-resp [res]
  (-> (response (f/serialize :json (:data res)))
      (header "content-location" (url (:resource_type res) (:logical_id res) (:version_id res)))
      (header "Last-Modified" (:last_modified_date res))))

(defn -create
  [{{rt :resource-type} :params body :body}]
  (let [body (slurp body)
        res (f/parse body)
        json (f/serialize :json res)
        item (db/-create rt json)]
    (-> (resource-resp res)
        (status 201))))

(defn -update
  [{{rt :resource-type id :id} :params body :body}]
  (let [body (slurp body)
        res (f/parse body)
        json (f/serialize :json res)
        item (db/-update rt id json)]
    (-> (resource-resp res)
        (status 200))))

(defn -delete
  [{{rt :resource-type id :id} :params body :body}]
  (-> (response (db/-delete rt id))
      (status 204)))

;;TODO add checks
(defn -read [{{rt :resource-type id :id} :params}]
  (let [res (db/-read rt id)]
    (-> (resource-resp res)
        (status 200))))

(cc/defroutes routes
  (cc/GET    "/"                                      []                 identity)
  (cc/GET    "/metadata"                              []                 #'-metadata)
  (cc/POST   "/:resource-type"                        [resource-type]    #'-create)
  (cc/GET    "/:resource-type/_search"                [resource-type]    #'-search)
  (cc/GET    ["/:resource-type/:id", :id uuid-regexp] [resource-type id] #'-read)
  (cc/GET    "/:resource-type/:id/_history"           [resource-type id] #'-history)
  (cc/DELETE "/:resource-type/:id"                    [resource-type id] #'-delete)
  (cc/PUT    "/:resource-type/:id"                    [resource-type id] #'-update)
  ; (cc/POST   "/:resource-type/_validate"              [resource-type id] 'sys-int/validate)
  ; (cc/GET    "/:resource-type/:id/_history/:vid"      [resource-type id vid] 'res-int/vread)
  (cr/files  "/"                                      {:root "resources/public"}))


(def app
  (-> routes
      (ch/api)))

(defn start-server []
  (jetty/run-jetty #'app {:port 3000 :join? false}))

(defn stop-server [server]
  (.stop server))
