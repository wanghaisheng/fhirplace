(ns fhirplace.core
  (:require [route-map :as rm]
            [compojure.handler :as ch]
            [ring.middleware.file :as rmf]
            [fhirplace.app]
            [ring.adapter.jetty :as jetty]))

(def GET :GET)
(def POST :POST)
(def PUT :PUT)
(def DELETE :DELETE)

(defn h [& hnds]
  (let [hnd (last hnds)
        mws (butlast hnds)]
    {:fn hnd :mw mws}))

;; TODO: remove (h)
(comment
  "should be like this"
  {GET =info
   "metadata" {GET  =metadata}
   [:type] {:mw ['<-outcome-on-exception  '->type-supported!]
            POST       [->valid-input!  =create]
            "_validate" {POST =validate}
            "_search"   {GET  =search}
            [:id] {:mw ['->resource-exists! '->check-deleted!]
                   GET       =read
                   DELETE    =delete
                   PUT       [->valid-input!
                              ->has-content-location!
                              ->has-latest-version!
                              =update]
                   [:vid]     {GET =vread}
                   "_history" {GET =history}}}})
(def routes
  {GET (h '=info)
   "metadata" {GET (h '=metadata)}
   [:type] {:mw ['<-outcome-on-exception  '->type-supported!]
            POST       (h '->valid-input!
                          '=create)
            "_validate" {POST (h '=validate)}
            "_search"   {GET  (h '=search)}
            [:id] {:mw ['->resource-exists! '->check-deleted!]
                   GET       (h '=read)
                   DELETE    (h '=delete)
                   PUT       (h '->valid-input!
                                '->has-content-location!
                                '->has-latest-version!
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

(def app (-> dispatch
             (resolve-handler)
             (resolve-route)
             (fhirplace.app/<-format)
             (ch/site)
             (rmf/wrap-file "resources/public")))

(defn start-server [] (jetty/run-jetty #'app {:port 3000 :join? false}))

(defn stop-server [server] (.stop server))
