(ns fhirplace.interactions.resource
  (:use ring.util.response
        ring.util.request)
  (:require [fhirplace.resources.operation-outcome :as oo]
            [fhirplace.repositories.resource :as repo]
            [fhirplace.util :as util]
            [clojure.data.json :as json]
            [clojure.string :as string])
  (:refer-clojure :exclude (read)))


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
  [{ {db :db :as system} :system, json-body :json-body, uri :uri
    {resource-type :resource-type} :params :as req}]
  (try
    (let [id (repo/insert db json-body)
          [{vid :version_id}] (repo/select-history db resource-type id)]
      (-> {}
          (header "Location" (util/cons-url system resource-type id vid))
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

;; 409/412 - version conflict management - see above
(defn update*
  [{{db :db :as system} :system {:keys [id resource-type]} :params
    body-str :body-str :as req}]
  (try
    (repo/update db id body-str)
    (let [[{vid :version_id}] (repo/select-history db resource-type id)
          resource-loc (util/cons-url system resource-type id vid)]
      (-> {}
          (header "Last-Modified" (java.util.Date.))
          (header "Location" resource-loc)
          (header "Content-Location" resource-loc)
          (status 200))) 
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

;;   DELETE
;; - If the server refuses to delete resources of that type on principle,
;;   then it should return the status code 405 method not allowed.
;; - If the server refuses to delete a resource because of reasons
;;   specific to that resource, such as referential integrity,
;;   it should return the status code 409 Conflict.
;; - Performing this interaction on a resource that is already deleted has no effect,
;;   and should return 204.

(defn delete
  [{{db :db} :system {:keys [id resource-type]} :params}]
  (if (repo/exists? db id)
    (-> (response (repo/delete db id))
        (status 204))
    {:status 404
     :body (oo/build-operation-outcome
             "fatal"
             (str "Resource with ID " id " doesn't exist"))}))

(defn wrap-with-existence-check [h]
  (fn [{{db :db} :system {id :id} :params :as req}]
    (if (repo/exists? db id)
      (h req)
      {:status 404
       :body (oo/build-operation-outcome
               "fatal"
               (str "Resource with ID " id " doesn't exist"))})))

(defn wrap-with-deleted-check [h]
  (fn [{{db :db} :system {id :id} :params :as req}]
    (if-not (repo/deleted? db id)
      (h req)
      {:status 410
       :body (oo/build-operation-outcome
               "warning"
               (str "Resource with ID " id " was deleted"))})))

(defn read*
  [{{db :db :as system} :system {:keys [id resource-type]} :params :as req}]
  (let [resource (repo/select db resource-type id)
        [{vid :version_id lmd :last_modified_date}] (repo/select-history db resource-type id)]
      {:status 200
       :headers {"Content-Location" (util/cons-url system resource-type id vid) 
                 "Last-Modified" lmd}
       :body resource}))

(def read
  (-> read*
      wrap-with-existence-check
      wrap-with-deleted-check))

(defn vread*
  [{{db :db} :system {:keys [resource-type id vid]} :params :as req}]
  (let [resource (repo/select-version db resource-type id vid)
        [{lmd :last_modified_date}] (repo/select-history db resource-type id)]
      {:status 200
       :headers {"Last-Modified" lmd}
       :body resource}))

(def vread
  (-> vread*
      wrap-with-existence-check
      wrap-with-deleted-check))
