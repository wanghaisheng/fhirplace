(ns fhirplace.handler
  (:use ring.util.response)
  (:require [fhirplace.core :as core]))

(defn create-handler
  "Handler for CREATE queries."
  [request]
  (-> (response "CREATE")
      (content-type "text/plain")))

(defn update-handler
  "Handler for UPDATE queries."
  [request]
  (-> (response "UPDATE")
      (content-type "text/plain")))

(defn delete-handler
  "Handler for DELETE queries."
  [request]
  (-> (response "DELETE")
      (content-type "text/plain")))

(defn read-handler
  "Handler for READ queries."
  [{ system :system params :params :as request }]
  (if-let [resource (core/select-resource (:db system) (:resource-type params) (:id params))]
    (-> (response (str resource))
        (content-type "text/json"))
    (-> (response "Not Found")
        (content-type "text/plain")
        (status 404)))
  )
