(ns fhirplace.core
  (:require [route-map :as rm]
            [compojure.handler :as ch]
            [ring.middleware.file :as rmf]
            [fhirplace.app]
            [ring.adapter.jetty :as jetty]
            [environ.core :as env]))


(def GET :GET)
(def POST :POST)
(def PUT :PUT)
(def DELETE :DELETE)

(defn h [& hnds]
  (let [hnd (last hnds)
        mws (butlast hnds)]
    {:fn hnd :mw mws}))

(def routes
  {GET (h '=info)
   "metadata" {GET (h '=metadata)}
   "Profile" { [:type] {GET (h '=profile)}}
   [:type] {:mw ['<-outcome-on-exception '->type-supported!]
            POST       (h '->parse-body! '->valid-input!  '=create)
            "_validate" {:mw ['->parse-body! '->valid-input!]
                         POST (h '=validate-create)
                         [:id] {POST (h '->latest-version! '=validate-update)}}
            "_search"   {GET  (h '=search)}
            [:id] {:mw ['->resource-exists! '->check-deleted!]
                   GET       (h '=read)
                   DELETE    (h '=delete)
                   PUT       (h '->parse-body!
                                '->latest-version!
                                '->valid-input!
                                '=update)
                   [:vid]     {GET (h '=vread)}
                   "_history" {GET (h '=history)}}}})

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

(defn build-stack
  "build stack from h - handler
  and mws - seq of middlewares"
  [h mws]
  (loop [h h [m & mws] (reverse mws)]
    (if m
      (recur (m h) mws) h)))

(defn dispatch [{handler :handler route :route :as req}]
  (let [mws  (map resolve-filter (collect :mw route))
        req  (update-in req [:params] merge (:params route))]
    (println "\n\nDispatching " (:request-method req) " " (:uri req) " to " (pr-str handler))
    (println "Middle-wares: " (pr-str mws))
    ((build-stack handler mws) req)))

(defn strip-context  [h]
  (fn  [{context :context uri :uri :as req}]
    (println req)
    (if-not context
      (h req)
      (let  [new-uri  (.substring uri  (.length context))]
        (h  (assoc req :uri new-uri))))))


(def app (-> dispatch
             (resolve-handler)
             (resolve-route)
             (fhirplace.app/<-format)
             (ch/site)
             (rmf/wrap-file "resources/public")
             (strip-context)))

(defn start-server []
  (jetty/run-jetty #'app {:port (env/env :fhirplace-web-port) :join? false}))

(defn stop-server [server] (.stop server))
