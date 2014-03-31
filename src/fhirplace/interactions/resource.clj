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

(defn delete
  "Handler for DELETE queries."
  [{ system :system params :params :as request }]
  (repo/delete (:db system) (:id params))

  (-> request
      (content-type "text/plain")
      (status 204)))

(defn read
  "Handler for READ queries."
  [{ system :system params :params :as request }]

  (if-let [resource (repo/select (:db system) (:resource-type params) (:id params))]
    (response resource)
    (-> (response "Not Found")
        (content-type "text/plain")
        (status 404))))
