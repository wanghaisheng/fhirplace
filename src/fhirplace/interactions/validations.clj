(ns fhirplace.interactions.validations
  (:use clojure.algo.monads
        ring.util.response
        ring.util.request)
  (:require [fhirplace.repositories.resource :as repo]
            [clojure.data.json :as json]))

(defmonad request-m
  [m-result identity
   m-bind (fn [req f]
            (if (nil? (:status req))
              (f req)
              req))])

(defn parse-json [req]
  "Trys to parse body to json. If success - 
  set body-json value of req. Otherwise - 400"
  (try
    (let [body-json (json/read-str (:body-str req))]
      (assoc req :body-json body-json))
    (catch Throwable e
      (status req 400))))

(defn check-type
  "Check if type is known"
  [{params :params system :system :as req}]
  (let [resource-types (repo/resource-types (:db system))
        resource-type (:resource-type params)]
    (if (contains? resource-types resource-type)
      req
      (status req 404))))

(defn check-existence 
  "Check for existing of resource by id.
  If resouce not found - returns 405."
  [{params :params system :system :as req}]
  (if (repo/exists? (:db system) (:id params))
    req
    (status req 405)))

(defn update-resource
  "Updates resource.
  TODO: if error occured, should return 422"
  [{ system :system params :params :as request }]
  (let [patient (:body-str request)]
    (try
      (repo/update (:db system) (:id params) patient)
      request
      (catch java.sql.SQLException e
        (status request 422)))))

(defn- construct-url
  [{scheme :scheme, remote-addr :remote-addr, uri :uri}, id]
  (str (name scheme) "://" remote-addr uri "/" id))

(defn pack-update-result
  "Pack update result as successfull."
  [{params :params :as req}]
  (-> req
      (header "Last-Modified" (java.util.Date.))
      (header "Location" (construct-url req (:id params)))
      (header "Content-Location" (construct-url req (:id params)))
      (status 200)))

(defmacro with-checks [& body]
  `(with-monad request-m 
     (m-chain [~@body])))


