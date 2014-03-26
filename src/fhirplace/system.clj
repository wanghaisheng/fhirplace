(ns fhirplace.system
  (:require
    [fhirplace.web :as web]
    [clojure.java.io :as io]))


(defn load-project-info []
  ( let [pr (->> "project.clj"
                 slurp
                 read-string)
         info (->> pr
                   (drop 2)
                   (cons :version)
                   (apply hash-map))]
    {:system-name (second pr)
     :version (:version info)}))

(defn load-config []
  (->> "config.clj"
       io/resource
       slurp
       read-string
       (merge (load-project-info))))

(defn create
  "Create system instance"
  []
  (load-config))

(defn start
  "Performs side effects to initialize the system, acquire resources,
  and start it running. Returns an updated instance of the system."
  [system]
  (assoc system :server
         (web/start-server
           (web/create-web-handler system)
           (:port system))))

(defn stop
  "Performs side effects to shut down the system and release its
  resources. Returns an updated instance of the system."
  [system]
  (when (:server system)
    (web/stop-server (:server system)))
  (dissoc system :server))
