(ns fhirplace.core
  (:require [route-map :as rm]
            [compojure.handler :as ch]
            [ring.middleware.file :as rmf]
            [fhirplace.app]
            [ring.adapter.jetty :as jetty]
            [environ.core :as env]))

(defn fhirplace-port
  []
  (or (env/env :fhirplace-web-port) 3000))

(def GET :GET)
(def POST :POST)
(def PUT :PUT)
(def DELETE :DELETE)

(def routes
  {GET {:fn '=info}
   "metadata" {GET {:fn '=metadata}}
   [:type] {:-> ['->type-supported!]
            :<- ['<-outcome-on-exception]
            POST       {:-> ['->valid-input!] :fn '=create}
            "_validate" {POST {:fn '=validate}}
            "_search"   {GET  {:fn '=search}}
            [:id] {:-> ['->resource-exists! '->check-deleted!]
                   GET       {:fn '=read}
                   DELETE    {:fn '=delete}
                   PUT       {:-> ['->valid-input! '->has-content-location! '->has-latest-version!]
                              :fn '=update}
                   "_history" {GET {:fn '=history}
                               [:vid] {GET {:fn '=hread}}}}}})

(defn match [meth path]
  (rm/match [meth path] routes))

(defn collect [k match]
  (filterv (complement nil?)
           (mapcat k (conj (:parents match) (:match match)))))

(defn resolve-route [h]
  (fn [{uri :uri meth :request-method :as req}]
    (if-let [route (match meth uri)]
      (h (assoc req :route route))
      {:status 404 :body (str "No route " meth " " uri)})))

(defn resolve-handler [h]
  (fn [{route :route :as req}]
    (let [handler-sym (get-in route [:match :fn])
          handler     (ns-resolve (find-ns 'fhirplace.app) handler-sym)]
      (if handler
        (h (assoc req :handler handler))
        {:status 500 :body (str "No handler " handler-sym)}))))

(defn- resolve-filter [nm]
  (if-let [fltr (ns-resolve (find-ns 'fhirplace.app) nm)]
    fltr
    (throw (Exception. (str "Could not resolve filter " nm)))))

(defn build-stack [h mws]
  (loop [h h [m & mws] (reverse mws)]
    (if m
      (recur (m h) mws) h)))

(defn dispatch [{handler :handler route :route :as req}]
  (let [filters  (map resolve-filter (collect :-> route))
        trans    (map resolve-filter (collect :<- route))
        req      (update-in req [:params] merge (:params route))]
    (println "Dispatching " (:request-method req) " " (:uri req) " to " (pr-str handler))
    (println "Filters " (pr-str filters))
    (println "Transformers " (pr-str trans))
    ((build-stack handler (concat trans filters)) req)))

(def app (-> dispatch
             (resolve-handler)
             (resolve-route)
             (fhirplace.app/<-format)
             (ch/site)
             (rmf/wrap-file "resources/public")))

(defn start-server [] (jetty/run-jetty #'app {:port (fhirplace-port) :join? false}))

(defn stop-server [server] (.stop server))
