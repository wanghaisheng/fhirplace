(ns fhirplace.interactions.resource
  (:use ring.util.response
        ring.util.request)
  (:require [fhirplace.repositories.resource :as repo]
            [fhirplace.util :as util])
  (:refer-clojure :exclude (read)))

(defn construct-url
  [{scheme :scheme, remote-addr :remote-addr, uri :uri}, id]
  (str (name scheme) "://" remote-addr uri "/" id))

(defn create
  "Handler for CREATE queries."
  [{ system :system params :params :as request }]
  (let [patient (body-string request)
        patient-id (repo/insert (:db system) patient)]
    (-> (redirect (str (:uri request) "/" patient-id))
        (status 201))))

(defn update
  "Handler for DELETE queries."
  [{ system :system params :params :as request }]
  (let [patient (body-string request)]
    (repo/update (:db system) (:id params) patient)
    (-> request
        (header "Last-Modified" (java.util.Date.))
        (content-type "text/plain")
        (status 200))))

(defn delete
  "Handler for CREATE queries."
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
