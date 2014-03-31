(ns fhirplace.interactions.resource
  (:use ring.util.response
        ring.util.request)
  (:require [fhirplace.repositories.resource :as repo]
            [fhirplace.interactions.validations :as valid]
            [fhirplace.util :as util]
            [clojure.data.json :as json]
            [clojure.algo.monads :as m])
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
(def create-with-checks (valid/with-checks
                          valid/parse-json           ;; 400
                          valid/check-type           ;; 404
                          valid/create-resource      ;; 422
                          )) ;; 201

(defn create
  "Handler for CREATE queries."
  [request]
  (:response
    (create-with-checks (assoc {} :request request :response {}))))

;; 400 Bad Request - resource could not be parsed or failed basic FHIR validation rules
;; 404 Not Found - resource type not supported, or not a FHIR end point
;; 405 Method Not allowed - the resource did not exist prior to the update,
;;                          and the serer does not allow client defined ids
;; 409/412 - version conflict management - see above
;; 422 Unprocessable Entity - the proposed resource violated applicable FHIR profiles
;;                            or server business rules. This should be accompanied by
;;                            an OperationOutcome resource providing additional detail
;; TODO: OperationOutcome
(def update-with-checks (valid/with-checks
                          valid/parse-json           ;; 400
                          valid/check-type           ;; 404
                          valid/check-existence      ;; 405
                          valid/update-resource))      ;; 422 or 200

(defn update
  "Handler for PUT queries."
  [request]
  (:response
    (update-with-checks (assoc {} :request request :response {}))))

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
(def delete-with-checks (valid/with-checks
                          valid/check-type
                          valid/check-existence
                          valid/delete-resource))
(defn delete
  "Handler for DELETE queries."
  [request]
  (:response
    (delete-with-checks (assoc {} :request request :response {}))))

;; 410 if resource was deleted.
;; If a request is made for a previous version of a resource,
;; and the server does not support accessing previous versions,
;; it should return a 405 Method Not Allowed error. 
(defn read
  "Handler for READ queries."
  [{ system :system params :params :as request }]

  (if-let [resource (repo/select (:db system) (:resource-type params) (:id params))]
    (response resource)
    (-> (response "Not Found")
        (content-type "text/plain")
        (status 404))))
