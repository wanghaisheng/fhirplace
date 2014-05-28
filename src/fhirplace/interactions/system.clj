(ns fhirplace.interactions.system
  (:use ring.util.response ring.util.request)
  (:require [fhirplace.resources.conformance :as conf]
            [fhir :as f]
            [ring.util.codec :as codec]
            [fhirplace.resources.bundle :as b]
            [fhirplace.resources.operation-outcome :as oo]
            [fhirplace.resources.conversion :as conversion]
            [fhirplace.resources.profiles :as frp]
            [fhirplace.resources.validation :as validation]
            [fhirplace.util :as util]
            [fhirplace.repositories.resource :as repo])
  (:refer-clojure :exclude (read)))

(defn conformance [_]
  "Handler for CONFORMANCE interaction."
  {:body (f/to-json (frp/profile :Conformance))})

(defn info
  "Handler for debug purposes (displays system info)."
  [{ system :system params :params :as request }]

  (-> (response [(str request)])))

(defn validate
  "Handler for validation of XML or JSON passed."
  [{{db :db :as system} :system {:keys [id resource-type]} :params body-str :body-str :as request}]
  (try
    (let [xml-resource (conversion/json->xml body-str)
          errors (validation/errors resource-type xml-resource)]
      ;;FIXME: errors should got to outcome
      (if errors
        {:status 422 :body errors}
        {:status 200 :body ""}))
    (catch Exception e
      {:status 400
       :body (oo/build-operation-outcome
              [{:severity "fatal" :details "Request body could not be parsed"}
               {:severity "fatal" :details (oo/exception-with-message e)}])})))

(defn- prepare-since [enc-since]
  (when enc-since
    (util/parse-time
     (codec/url-decode enc-since))))

(defn history
  [{{db :db :as system} :system {:keys [id resource-type _count _since]} :params}]
  (let [since (prepare-since _since)
        cnt (when _count (Integer. _count))]
    (if (repo/exists? db id)
      {:body (b/build-history
              (repo/select-history db resource-type id cnt since)
              system)}
      {:status 404
       :body (oo/build-operation-outcome
              "fatal"
              (str "Resource with ID " id " doesn't exist"))})))
