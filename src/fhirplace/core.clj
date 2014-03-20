(ns fhirplace.core
  (:use ring.util.response))

(defn app-handler
  "Root request handler."
  [request]
  (-> (response "Hello World")
    (content-type "text/plain")))

