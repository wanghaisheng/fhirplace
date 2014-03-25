(ns fhirplace.handler
  (:use ring.util.response
        ring.util.request
        fhirplace.core)
  (:require [fhirplace.core :as core]
            [clojure.data.json :as json]))

(defn construct-url 
  [{scheme :scheme, remote-addr :remote-addr, uri :uri}, id]
  (str (name scheme) "://" remote-addr uri "/" id))

(defn create-handler
  "Handler for CREATE queries."
  [{ system :system params :params :as request }]
  (let [patient (body-string request)
        patient-id (insert-resource (:db system) patient)]
    (-> request
        (header "Location" (construct-url request patient-id))
        (content-type "text/plain")
        (status 200))))


(defn update-handler
  "Handler for DELETE queries."
  [{ system :system params :params :as request }]
  (let [patient (body-string request)]
    (update-resource (:db system) (:id params) patient)
    (-> request
        (header "Last-Modified" (java.util.Date.))
        (content-type "text/plain")
        (status 200))))

(defn delete-handler
  "Handler for CREATE queries."
  [{ system :system params :params :as request }]
  (delete-resource (:db system) (:id params))
  (-> request
      (content-type "text/plain")
      (status 204)))

(defn read-handler
  "Handler for READ queries."
  [{ system :system params :params :as request }]
  (if-let [resource (core/select-resource (:db system) (:resource-type params) (:id params))]
    (-> (response (str resource))
        (content-type "text/json"))
    (-> (response "Not Found")
        (content-type "text/plain")
        (status 404))))
