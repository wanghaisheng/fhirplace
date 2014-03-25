(ns fhirplace.app
  (:use compojure.core)
  (:require [fhirplace.handler :as fhandler]
            [fhirplace.core :as core]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [fhirplace.interactions.system :as sysint]
            [ring.middleware.json :refer :all]))

(def uuid-regexp
  #"[0-f]{8}-([0-f]{4}-){3}[0-f]{12}")

(defroutes main-routes
  (GET    "/metadata"                              []                 sysint/conformance)
  (POST   "/:resource-type"                        [resource-type]    fhandler/create-handler)
  (GET    ["/:resource-type/:id", :id uuid-regexp] [resource-type id] fhandler/read-handler)
  (DELETE "/:resource-type/:id"                    [resource-type id] fhandler/delete-handler)
  (PUT    "/:resource-type/:id"                    [resource-type id] fhandler/update-handler)
  (route/not-found "Not Found"))

(def app
  (handler/site main-routes))

(defn wrap-with-system
  [handler system]
  (fn [request]
    (handler (assoc request :system system))))

(defn create-web-handler [system]
  (let [app (handler/site main-routes)]
    (-> app
      (wrap-with-system system)
      (wrap-json-response {:pretty true}))))
