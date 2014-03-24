(ns fhirplace.app
  (:use compojure.core)
  (:require [fhirplace.handler :as fhandler]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]))

(def uuid-regexp
  #"[0-f]{8}-([0-f]{4}-){3}[0-f]{12}")

(comment def resource-types-regexp
  (re-pattern (str "(" (clojure.string/join "|" (core/resource-types)) ")")))

(defroutes main-routes
  (POST "/:resource-type" [resource-type] fhandler/create-handler)
  (GET ["/:resource-type/:id", :id uuid-regexp] [resource-type id] fhandler/read-handler)
  (DELETE "/:resource-type/:id" [resource-type id] fhandler/delete-handler)
  (PUT "/:resource-type/:id" [resource-type id] fhandler/update-handler)
  (route/not-found "Not Found"))

(def app
  (handler/site main-routes))
