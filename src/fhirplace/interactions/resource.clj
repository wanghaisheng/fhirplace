(ns fhirplace.interactions.resource
  (:use ring.util.response
        ring.util.request)
  (:require [fhirplace.resources.operation-outcome :as oo]
            [fhirplace.repositories.resource :as repo]
            [fhirplace.util :as util]
            [clojure.data.json :as json]
            [clojure.string :as string])
  (:refer-clojure :exclude (read)))

(defn construct-url
  [{scheme :scheme, remote-addr :remote-addr, uri :uri}, id]
  (str (name scheme) "://" remote-addr uri "/" id))


;; 400 Bad Request - resource could not be parsed or failed basic FHIR validation rules
;; 404 Not Found - resource type not supported, or not a FHIR end
;;                 point
;; 422 Unprocessable Entity - the proposed resource violated
;;              applicable FHIR profiles or server business rules. This should be
;;              accompanied by an OperationOutcome resource providing additional
;;              detail
(defn wrap-with-json [h]
  (fn [{body-str :body-str :as req}]
    (try
      (let  [json-body  (json/read-str body-str)]
        (h (assoc req :json-body json-body)))
      (catch Exception e
        {:status 400 :message (str e)}))))

(defn check-type? [db type]
  (let [resource-types (map string/lower-case
                              (repo/resource-types db))]
    (contains? (set resource-types) (string/lower-case type))))

(defn wrap-with-check-type [h]
  (fn [{{db :db} :system {resource-type :resource-type} :params :as req}]
    (if (check-type? db resource-type)
      (h req)
      {:status 404
       :body (oo/build-operation-outcome
               "fatal"
               (str "Resource type " resource-type " isn't supported"))})))

(defn create*
  [{ {db :db} :system, json-body :json-body :as req}]
  (try
    (let [id (repo/insert db json-body)]
      (-> {}
          (header "Location" (construct-url req id))
          (status 201)))
    (catch java.sql.SQLException e
      {:status 422
       :body (oo/build-operation-outcome
               "fatal"
               "Insertion of resource has failed on DB server")})))

(def create
  (-> create*
       wrap-with-check-type
       wrap-with-json))

(defn wrap-resource-not-exist [h status]
  (fn [{{db :db} :system, {id :id} :params :as req}]
    (if (repo/exists? db id)
      (h req)
      {:status status
       :body (oo/build-operation-outcome
               "fatal"
               (str "Resource with ID " id " doesn't exist"))})))

;; 400 Bad Request - resource could not be parsed or failed basic FHIR validation rules
;; 404 Not Found - resource type not supported, or not a FHIR end point
;; 405 Method Not allowed - the resource did not exist prior to the update,
;;                          and the serer does not allow client defined ids
;; 409/412 - version conflict management - see above
;; 422 Unprocessable Entity - the proposed resource violated applicable FHIR profiles
;;                            or server business rules. This should be accompanied by
;;                            an OperationOutcome resource providing additional detail
;; TODO: OperationOutcome
(defn update*
  [{{db :db} :system, {id :id} :params
    body-str :body-str, :as req}]
  (try
    (repo/update db id body-str)
    (-> {}
        (header "Last-Modified" (java.util.Date.))
        (header "Location" (construct-url req id))
        (header "Content-Location" (construct-url req id))
        (status 200))
    (catch java.sql.SQLException e
      {:status 422
       :body (oo/build-operation-outcome
               "fatal"
               "Update of resource has failed on DB server")})))

(def update
  (-> update*
      (wrap-resource-not-exist 405)
      wrap-with-check-type
      wrap-with-json))

;; DELETE
;; - (Done) Upon successful deletion the server should return 204  (No Content).
;; - If the server refuses to delete resources of that type on principle,
;;   then it should return the status code 405 method not allowed.
;; - If the server refuses to delete a resource because of reasons
;;   specific to that resource, such as referential integrity,
;;   it should return the status code 409 Conflict.
;; - (Done) If the resource cannot be deleted because it does not exist on the server,
;;   the server SHALL return 404  (Not found))
;; - Performing this interaction on a resource that is already deleted has no effect,
;    and should return 204.

(defn delete
  [{{db :db} :system {:keys [id resource-type]} :params}]
  (if (repo/exists? db id)
    (->
      (response (repo/delete db id))
      (status 204))
    {:status 404
     :body (oo/build-operation-outcome
             "fatal"
             (str "Resource with ID " id " doesn't exist"))}))

;; 410 if resource was deleted.
;; If a request is made for a previous version of a resource,
;;   and the server does not support accessing previous versions,
;; it should return a 405 Method Not Allowed error.
;; A GET for a deleted resource returns a 410 status code.

(defn read
  [{{db :db} :system {:keys [id resource-type]} :params}]
  (if (repo/exists? db id)
    (response
      (repo/select db resource-type id))
    {:status 404
     :body (oo/build-operation-outcome
             "fatal"
             (str "Resource with ID " id " doesn't exist"))}))

(defn vread
    [{{db :db} :system {:keys [resource-type id vid]} :params}]
     (response (repo/select-version db resource-type id vid)))
