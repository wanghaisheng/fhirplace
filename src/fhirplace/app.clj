(ns fhirplace.app
  (:use compojure.core)
  (:require [fhirplace.handler :as fhandler]
            [fhirplace.core :as core]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(def uuid-regexp
  #"[0-f]{8}-([0-f]{4}-){3}[0-f]{12}")

;; (def resource-types-regexp
;;   (re-pattern (str "(" (clojure.string/join "|" (map clojure.string/lower-case (core/resource-types))) ")")))

;; TODO: Handle non-existed resource types
(defroutes main-routes
  (POST   "/:resource-type"                        [resource-type]    fhandler/create-handler)
  (GET    ["/:resource-type/:id",
           :id uuid-regexp]                        [resource-type id] fhandler/read-handler)
  (DELETE "/:resource-type/:id"                    [resource-type id] fhandler/delete-handler)
  (PUT    "/:resource-type/:id"                    [resource-type id] fhandler/update-handler)
  (route/not-found "Not Found"))

(defn create-web-handler [system]
  (let [app (handler/site main-routes)]
    (fn [request]
      (app (assoc request :system system)))))
