(ns fhirplace.interactions.system
  (:use ring.util.response ring.util.request)
  (:require [fhirplace.resources.conformance :as conf]
            [ring.util.codec :as codec]
            [fhirplace.resources.history :as hist]
            [fhirplace.resources.operation-outcome :as oo]
            [clj-time.format :as time]
            [clj-time.coerce :as time-coerce]
            [fhirplace.resources.conversion :as conversion]
            [fhirplace.resources.validation :as validation]
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
              "fatal" "Request body could not be parsed")})))

(defn- to-sql-time [since]
  (when since
    (let [formatter (time/formatter "YYYY-MM-dd HH:mm:ss.SSSSSSZZ")]
      (time-coerce/to-sql-time
       (time/parse formatter since)))))

(defn history
  [{{db :db :as system} :system {:keys [id resource-type _count _since]} :params}]
  (let [since-decoded (when _since (codec/url-decode _since))
        since-sql (to-sql-time since-decoded)
        cnt (when _count (Integer. _count))]
    (if (repo/exists? db id)
      {:body (hist/build-history
              (repo/select-history db resource-type id cnt since-sql)
              system)}
      {:status 404
       :body (oo/build-operation-outcome
              "fatal"
              (str "Resource with ID " id " doesn't exist"))})))
