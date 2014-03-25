(ns fhirplace.server
    (:require [ring.adapter.jetty :as jetty]))

(defn start
    [handler port]
    {:pre [(not (nil? port))
           (not (nil? handler))]}
    (jetty/run-jetty handler {:port port :join? false}))

(defn stop
    [server]
    (.stop server))
