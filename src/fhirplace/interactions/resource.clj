(ns fhirplace.interactions.resource
  (:use ring.util.response
        ring.util.request)
  (:require [fhirplace.repositories.resource :as repo]
            [fhirplace.util :as util]
            [clojure.data.json :as json])
  (:refer-clojure :exclude (read)))

(defn construct-url
  [{scheme :scheme, remote-addr :remote-addr, uri :uri}, id]
  (str (name scheme) "://" remote-addr uri "/" id))

(defn- safely-parse-json [json-str]
  (try
    (json/read-str json-str :key-fn keyword)
    (catch Exception e nil)))

(defn create
  "Handler for CREATE queries."
  [{ system :system params :params :as request }]

  (let [resource (body-string request)
        resource-json (safely-parse-json resource)]
    (if resource-json
      (-> (redirect (str (:uri request) "/" resource-id))
        (status 201))
      (status request 400))))

(defn update
  "Handler for UPDATE queries."
  [{ system :system params :params :as request }]
  (let [resource (body-string request)
        resource-json (safely-parse-json resource)]
    (repo/update (:db system) (:id params) resource)

    (if resource-json
      (-> request
        (header "Last-Modified" (java.util.Date.))
        (status 200))

      (status request 422))))

(defn delete
  "Handler for DELETE queries."
  [{ system :system params :params :as request }]
  (repo/delete (:db system) (:id params))

  (-> request
      (content-type "text/plain")
      (status 204)))

(defn read
  "Handler for READ queries."
  [{ system :system params :params :as request }]

  (if-let [resource (repo/select (:db system) (:resource-type params) (:id params))]
    (response resource)
    (-> (response "Not Found")
        (content-type "text/plain")
        (status 404))))
