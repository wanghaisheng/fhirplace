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

(defn parse-json
  "Trys to parse body to json. If success -
  set body-json value of req. Otherwise - 400"
 [{request :request, response :response :as message}]
  (try
    (let [body-json (json/read-str (:body-str request))]
      (assoc-in message [:request :body-json] body-json))
    (catch Throwable e
      (assoc-in message [:response :status] 400))))

(defn check-type
  "Check if type is known"

  [{ {:keys [params system]} :request,
    response :response :as message }]

  (let [resource-types (set 
                         (map str/lower-case
                              (repo/resource-types (:db system))))

        resource-type (str/lower-case (:resource-type params))]
    (if (contains? resource-types resource-type)
      message
      (assoc-in message [:response :status] 404))))

(defn check-existence
  "Check for existing of resource by id.
  If resouce not found - returns 405."

  [{ {:keys [params system request-method] :as request} :request,
    response :response :as message }]

  (if (repo/exists? (:db system) (:id params))
    message
    (assoc-in message [:response :status] 405)))

(defn create-resource
  "Creates new resource, if FHIRBase reports an error,
   returns 422 HTTP status"

  [{ {:keys [params system body-str] :as request} :request,
    response :response :as message }]

  (let [resource body-str]
    (try
      (let [id (repo/insert (:db system) resource)]
        (-> response
            (header "Location" (construct-url request id))
            (status 201)
            (#(assoc message :response %))))

      (catch java.sql.SQLException e
        (assoc-in message [:response :status] 422)))))

(defn update-resource
  "Updates resource.
  TODO: if error occured, should return 422"

  [{ {:keys [params system body-str] :as request} :request,
    response :response :as message }]

  (try
    (repo/update (:db system) (:id params) body-str)
    (-> response
        (header "Last-Modified" (java.util.Date.))
        (header "Location" (construct-url request (:id params)))
        (header "Content-Location" (construct-url request (:id params)))
        (status 200)
        (#(assoc message :response %)))
    (catch java.sql.SQLException e
      (assoc-in message [:response :status] 422))))

(defn delete-resource
  "Deletes resource and return 204.
  If error occured - 500."

  [{ {:keys [params system body-str] :as request} :request,
    response :response :as message }]
  
  (try
    (repo/delete (:db system) (:id params))
    (assoc-in message [:response :status] 204)
    
    (catch java.sql.SQLException e
      (assoc-in message [:response :status] 500))))

(defmonad request-m
  [m-result identity
   m-bind (fn [message f]
            (if (nil? (get-in message [:response :status]))
              (f message)
              message))])

(defmacro with-checks [& body]
  `(with-monad request-m
     (m-chain [~@body])))

;;(defn with-checks [& fns]
;;  (fn [request]
;;    (some
;;      (fn [f]
;;        (let [resp (f request)
;;              status (get-in resp [:response :status])]
;;          (if status response nil)))
;;      fns)))
