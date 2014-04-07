(ns fhirplace.interactions.system
  (:use ring.util.response ring.util.request)
  (:require [fhirplace.resources.conformance :as conf]
            [fhirplace.resources.history :as hist]
            [fhirplace.resources.operation-outcome :as oo]
            [fhirplace.repositories.resource :as repo]
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

(defn history
  [{{db :db :as system} :system {:keys [id resource-type _count]} :params}]
  (if (repo/exists? db id)
    {:body (hist/build-history
             (repo/select-history db resource-type id _count)
             system)}
    {:status 404
     :body (oo/build-operation-outcome
             "fatal"
             (str "Resource with ID " id " doesn't exist"))}))
