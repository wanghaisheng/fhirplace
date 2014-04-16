(ns fhirplace.interactions.resource
  (:use ring.util.response
        ring.util.request)
  (:require [fhirplace.resources.operation-outcome :as oo]
            [fhirplace.repositories.resource :as repo]
            [fhirplace.resources.conversion :as conv]
            [fhirplace.util :as util]
            [clojure.data.json :as json]
            [clojure.string :as string])
  (:refer-clojure :exclude (read)))

(defn wrap-with-json [h]
  (fn [{body-str :body-str {format :_format} :params :as req}]
    (try
      (let [json-body (cond
                       (util/format-json? format) (json/read-str body-str)
                       (util/format-xml? format)  (conv/xml->json body-str)
                       ;;TODO return OUTCOME
                       :else {})]
        (h (assoc req :json-body json-body)))
      (catch Exception e
        {:status 400
         :body (oo/build-operation-outcome
                 "fatal"
                 (str "Resource cannot be parsed"))}))))

(defn- check-type [db type]
  (let [resource-types (map string/lower-case
                            (repo/resource-types db))]
    (contains? (set resource-types) (string/lower-case type))))

(defn wrap-with-check-type [h]
  (fn [{{db :db} :system {resource-type :resource-type} :params :as req}]
    (if (check-type db resource-type)
      (h req)
      {:status 404
       :body (oo/build-operation-outcome
              "fatal"
              (str "Resource type " resource-type " isn't supported"))})))

(defn wrap-resource-not-exist [h status]
  (fn [{{db :db} :system, {id :id} :params :as req}]
    (if (repo/exists? db id)
      (h req)
      {:status status
       :body (oo/build-operation-outcome
               "fatal"
               (str "Resource with ID " id " doesn't exist"))})))

(defn wrap-resource-has-content-location [h]
  (fn [{{content-location "Content-Location"} :headers :as req}]
    (if content-location
      (h req)
      {:status 412
       :body (oo/build-operation-outcome
               "fatal"
               (str "Version id is missing in content location header"))})))

(defn wrap-resource-has-latest-version [h]
  (fn [{{db :db} :system
        {id :id resource-type :resource-type} :params
        {content-location "Content-Location"} :headers :as req}]
    (let [version-id (last (string/split content-location #"_history/"))
          last-version-id (repo/select-latest-version-id db resource-type id)]
      (if (= version-id last-version-id)
        (h req)
        {:status 409
         :body (oo/build-operation-outcome
                 "fatal"
                 (str "Version id is not equal to latest version '" last-version-id "'"))}))))

(defn wrap-with-existence-check [h]
  (fn [{{db :db} :system {id :id} :params :as req}]
    (if (repo/exists? db id)
      (h req)
      {:status 404
       :body (oo/build-operation-outcome
               "fatal"
               (str "Resource with ID " id " doesn't exist"))})))

(defn check-if-deleted [h status]
  (fn [{{db :db} :system {id :id} :params :as req}]
    (if-not (repo/deleted? db id)
      (h req)
      {:status status
       :body (oo/build-operation-outcome
               "warning"
               (str "Resource with ID " id " was deleted"))})))

(defn create*
  [{ {db :db :as system} :system, json-body :json-body, uri :uri
    {resource-type :resource-type} :params :as req}]
  (try
    (let [id (repo/insert db json-body)
          vid (repo/select-latest-version-id db resource-type id)]
      (-> {}
          (header "Location" (util/cons-url system resource-type id vid))
          (status 201)))
    (catch java.sql.SQLException e
      {:status 422
       :body (oo/build-operation-outcome
               "fatal"
               "Insertion of resource has failed on DB server")})))

(defmacro <- [& forms]
  `(-> ~@(reverse forms)))

(def create
  (<- wrap-with-json
      wrap-with-check-type
      create*))

;; 409/412 - version conflict management - see above
(defn update*
  [{{db :db :as system} :system {:keys [id resource-type]} :params
    body-str :body-str :as req}]
  (try
    (repo/update db id body-str)
    (let [vid (repo/select-latest-version-id db resource-type id)
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
  (<- wrap-resource-has-content-location
      wrap-resource-has-latest-version
      wrap-with-json
      wrap-with-check-type
      (wrap-resource-not-exist 405)
      update*))

;;   DELETE
;; - If the server refuses to delete resources of that type on principle,
;;   then it should return the status code 405 method not allowed.
;; - If the server refuses to delete a resource because of reasons
;;   specific to that resource, such as referential integrity,
;;   it should return the status code 409 Conflict.
;; - Performing this interaction on a resource that is already deleted has no effect,
;;   and should return 204.

(defn delete*
  [{{db :db} :system {id :id} :params}]
  (-> (response (repo/delete db id))
      (status 204)))

(def delete
  (<- (check-if-deleted 204)
      (wrap-resource-not-exist 404)
      delete*))

(defn read*
  [{{db :db :as system} :system
    {:keys [id resource-type]} :params
    :as req}]

  (let [res (repo/select-latest-version db resource-type id)]
    {:status 200
     :headers {"Content-Location" (util/cons-url system resource-type id (:version_id res))
               "Last-Modified" (:last_modified_date res)}
     :body (:data res)}))

(def read
  (<- (check-if-deleted 410)
      wrap-with-existence-check
      read*))

(defn vread*
  [{{db :db} :system {:keys [resource-type id vid]} :params :as req}]
  (let [{lmd :last-modified-date
         resource :data} (repo/select-version db resource-type id vid)]
    {:status 200
     :headers {"Last-Modified" lmd}
     :body resource}))

(def vread
  (<- (check-if-deleted 410)
      wrap-with-existence-check
      vread*))


;;FIXME: temporal
(defn search
  [{{db :db} :system {:keys [resource-type]} :params}]
  {:status 200
   :body (repo/search db resource-type)})

