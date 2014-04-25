(ns fhirplace.resources.history
  (:require [fhirplace.util :as util]
           [fhirplace.resources.bundle :as b]))

(defn build-history [entries system]
  (-> (b/build-bundle entries system)
      (assoc :title "History of Resource")
      ;; (update-in [:link] conj {:rel "self" :href "TODO"})
      ))
