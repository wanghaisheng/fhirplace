(ns fhirplace.app
  (:require [fhirplace.handler :as handler])
  (:use (ring.middleware file content-type not-modified params)))

(def app
  (-> handler/root-handler
    (wrap-params)
    (wrap-file ".")))
