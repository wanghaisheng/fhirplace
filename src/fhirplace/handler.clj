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
  [{ params :params :as request }]
  (if-let [resource (core/select-resource (:resource-type params) (:id params))]
    (-> (response resource)
        (content-type "text/json"))
    (-> (response "Not Found")
        (content-type "text/plain")
        (status 404))))

(def uuid-regexp
  #"[0-f]{8}-([0-f]{4}-){3}[0-f]{12}")

(def resource-types-regexp
  (re-pattern (str "(" (clojure.string/join "|" (core/resource-types)) ")")))
