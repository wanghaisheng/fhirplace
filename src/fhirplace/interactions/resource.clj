(ns fhirplace.interactions.resource
  (:use ring.util.response
        ring.util.request)
  (:require [fhirplace.resources.operation-outcome :as oo]
            [fhirplace.repositories.resource :as repo]
            [fhirplace.resources.conversion :as conv]
            [fhirplace.db :as db]
            [fhirplace.util :as util]
            [clojure.data.json :as json]
            [fhirplace.json :as fj]
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
                 [{:severity "fatal"
                   :details "Resource cannot be parsed"}
                  {:severity "fatal"
                   :details (oo/exception-with-message e)}])}))))

(defn- check-type [type]
  ;; TODO: check types from profile
  true)

(defn wrap-with-check-type [h]
  (fn [{{db :db} :system {resource-type :resource-type} :params :as req}]
    (if (check-type resource-type)
      (h req)
      {:status 404
       :body (oo/build-operation-outcome
               "fatal"
               (str "Resource type " resource-type " isn't supported"))})))

(defn wrap-resource-not-exist [h status]
  (fn [{{db :db} :system, {id :id} :params :as req}]
    (if (db/exists? id)
      (h req)
      {:status status
       :body (oo/build-operation-outcome
               "fatal"
               (str "Resource with ID " id " doesn't exist"))})))

(defn wrap-resource-has-content-location [h]
  (fn [{{content-location "content-location"} :headers :as req}]
    (if content-location
      (h req)
      {:status 412
       :body (oo/build-operation-outcome
               "fatal"
               (str "Version id is missing in content location header"))})))

(defn wrap-resource-has-latest-version [h]
  (fn [{{db :db} :system
        {id :id resource-type :resource-type} :params
        {content-location "content-location"} :headers :as req}]
    (let [version-id (last (string/split content-location #"_history/"))
          meta (repo/select-latest-metadata db resource-type id)
          last-version-id (str (:version_id meta))]
      (if (= version-id last-version-id)
        (h req)
        {:status 409
         :body (oo/build-operation-outcome
                 "fatal"
                 (str "Version id '"
                      version-id
                      "' is not equal to latest version '"
                      last-version-id "'"))}))))

(defn check-if-deleted [h status]
  (fn [{{db :db} :system {id :id} :params :as req}]
    (if-not false ;;(repo/deleted? db id)
      (h req)
      {:status status
       :body (oo/build-operation-outcome
               "warning"
               (str "Resource with ID " id " was deleted"))})))

(defn- last-modified-header [response ^java.util.Date lmd]
  (->> lmd
       (util/unparse-time)
       (header response "Last-Modified")))

(defn create*
  [{system :system json-body :body-str, uri :uri {resource-type :resource-type} :params :as req}]
  (try
    (let [meta (db/create-resource resource-type json-body)
          id  (:logical_id meta)
          vid (:version_id meta)
          lmd (:last_modified_date meta)]
      (-> {:body meta}
          (header "Location" (util/cons-url system resource-type id  vid))
          (last-modified-header lmd)
          (status 201)))
    (catch java.sql.SQLException e
      {:status 422
       :body (oo/build-operation-outcome
               [{:severity "fatal"
                 :details "Insertion of resource has failed on DB server"}
                {:severity "fatal"
                 :details (oo/exception-with-message e)}])})))

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
    (let [meta (repo/select-latest-metadata db resource-type id)
          vid (:version_id meta)
          lmd (:last_modified_date meta)
          resource-loc (util/cons-url system resource-type id vid)]
      (-> {}
          (last-modified-header lmd)
          (header "Location" resource-loc)
          (header "content-location" resource-loc)
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
  [{system :system
    {:keys [id resource-type]} :params
    :as req}]
  (let [res (db/find-resource resource-type id)]
    (-> (response (:data res))
        (header "content-location"
                (util/cons-url system resource-type id (:version_id res)))
        (last-modified-header (:last_modified_date res))
        (status 200))))

(def read
  (<- (check-if-deleted 410)
      (wrap-resource-not-exist 404)
      read*))

(defn vread*
  [{{db :db} :system {:keys [resource-type id vid]} :params :as req}]
  (let [{lmd :last_modified_date
         resource :data} (repo/select-version db resource-type id vid)]
    (-> (response resource)
        (last-modified-header lmd)
        (status 200))))

(def vread
  (<- (check-if-deleted 410)
      (wrap-resource-not-exist 404)
      vread*))

(defn search
  [{{:keys [resource-type]} :params}]
  {:status 200
   :body (fj/generate (db/search resource-type))})
