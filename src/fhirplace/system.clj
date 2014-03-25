(ns fhirplace.system
  (:require
    [fhirplace.server :as server]
    [fhirplace.app :as app]))

(defn create
  "Create system instance"
  []
  {:port 8889
   :db {:subprotocol "postgresql"
        :subname "//127.0.0.1:5432/medsaless"
        :password "nicola"
        :user "nicola"}})

(defn start
  "Performs side effects to initialize the system, acquire resources,
  and start it running. Returns an updated instance of the system."
  [system]
  (assoc system :server
         (server/start
           (app/create-web-handler system)
           (:port system))))

(defn stop
  "Performs side effects to shut down the system and release its
  resources. Returns an updated instance of the system."
  [system]
  (when (:server system)
    (server/stop (:server system)))
  (dissoc system :server))

