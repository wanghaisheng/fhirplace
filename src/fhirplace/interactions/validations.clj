(ns fhirplace.interactions.validations
  (:use clojure.algo.monads
        ring.util.response
        ring.util.request)
  (:require [fhirplace.repositories.resource :as repo]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(defn- construct-url
  [{scheme :scheme, remote-addr :remote-addr, uri :uri}, id]
  (str (name scheme) "://" remote-addr uri "/" id))

(defn parse-json [request]
  "Trys to parse body to json. If success -
  set body-json value of req. Otherwise - 400"
  (try
    (let [body-json (json/read-str (:body-str request))]
      (assoc request :body-json body-json))
    (catch Throwable e
      (status request 400))))

(defn check-type
  "Check if type is known"
  [{params :params system :system :as request}]
  (let [resource-types (set (map
                              str/lower-case
                              (repo/resource-types (:db system))))

        resource-type (str/lower-case (:resource-type params))]
    (if (contains? resource-types resource-type)
      request
      (status request 404))))

(defn check-existence
  "Check for existing of resource by id.
  If resouce not found - returns 405."
  [{params :params system :system :as request}]
  (if (repo/exists? (:db system) (:id params))
    request
    (status request 405)))

(defn create-resource
  "Creates new resource, if FHIRBase reports an error,
   returns 422 HTTP status"
  [{params :params system :system :as request}]
  (let [resource (:body-str request)]
    (try
      (repo/insert (:db system) resource)
      (-> request
        (header "Location" (construct-url request (:id params)))
        (status 200))

      (catch java.sql.SQLException e
        (status {} 422)))))

(defn update-resource
  "Updates resource.
  TODO: if error occured, should return 422"
  [{ system :system params :params :as request }]
  (let [patient (:body-str request)]
    (try
      (repo/update (:db system) (:id params) patient)
      (-> {}
        (header "Last-Modified" (java.util.Date.))
        (header "Location" (construct-url request (:id params)))
        (header "Content-Location" (construct-url request (:id params)))
        (status 200))
      (catch java.sql.SQLException e
        (status {} 422)))))

(defn pack-update-result
  "Pack update result as successfull."
  [{params :params :as request}]
  (-> request
      (header "Last-Modified" (java.util.Date.))
      (header "Location" (construct-url request (:id params)))
      (header "Content-Location" (construct-url request (:id params)))
      (status 200)))

(defmonad request-m
  [m-result identity
   m-bind (fn [request f]
            (if (nil? (:status request))
              (f request)
              request))])

;; (defmacro with-checks [& body]
;;   `(with-monad request-m
;;      (m-chain [~@body])))

(defn with-checks [& fns]
  (fn [request]
    (some
      (fn [f]
        (let [resp (f request)
              status (:status resp)]
          (if status response nil)))
      fns)))
