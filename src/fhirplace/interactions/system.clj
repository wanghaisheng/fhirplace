(ns fhirplace.interactions.system
  (:use ring.util.response ring.util.request)
  (:require [fhirplace.resources.conformance :as conf]
            [fhirplace.repositories.resource :as repo])

  (:refer-clojure :exclude (read)))

(defn conformance
  "Handler for CONFORMANCE interaction."
  [{ system :system params :params :as request }]

  {:body (conf/build-conformance
           (repo/resource-types (:db system))
           system)})

(defn info
  "Handler for debug purposes (displays system info)."
  [{ system :system params :params :as request }]

  (-> (response [(str request)])))
