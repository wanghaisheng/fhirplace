(ns fhirplace.web
  (:use compojure.core)
  (:require [fhirplace.interactions.resource :as res-int]
            [fhirplace.interactions.system :as sys-int]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [ring.middleware.json :refer :all]
            [ring.middleware.stacktrace :refer :all]
            [ring.adapter.jetty :as jetty]))

(def uuid-regexp
  #"[0-f]{8}-([0-f]{4}-){3}[0-f]{12}")

;; TODO: Handle non-existed resource types
(defroutes main-routes
  (GET    "/info"                                  []                 sys-int/info)
  (GET    "/metadata"                              []                 sys-int/conformance)
  (POST   "/:resource-type"                        [resource-type]    res-int/create)
  (GET    ["/:resource-type/:id", :id uuid-regexp] [resource-type id] res-int/read)
  (DELETE "/:resource-type/:id"                    [resource-type id] res-int/delete)
  (PUT    "/:resource-type/:id"                    [resource-type id] res-int/update)
  (route/not-found "Not Found"))

(defn wrap-with-system
  [handler system]
  (fn [request]
    (handler (assoc request :system system))))

(defn create-web-handler [system]
  (let [stacktrace-fn (if (= :dev (:env system)) (wrap-stacktrace) identity)]
    (stacktrace-fn (-> (handler/site main-routes)
                     (wrap-with-system system)
                     (wrap-json-response {:pretty true})))))

(defn start-server
  [handler port]
  {:pre [(not (nil? port))
         (not (nil? handler))]}
  (jetty/run-jetty handler {:port port :join? false}))

(defn stop-server
  [server]
  (.stop server))
