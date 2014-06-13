(ns production
  (:require [immutant.web :as web]
            [fhirplace.core :as fcore]))

(defn init []
  (web/start "/" fcore/app))
