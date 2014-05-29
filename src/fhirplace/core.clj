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

(def routes
  {GET {:fn '=info}
   "metadata" {GET {:fn '=metadata}}
   [:type] {:-> ['->type-supported!]
            :<- ['<-format '<-outcome-on-exception]
            POST       {:-> ['->valid-input!] :fn '=create}
            "_validate" {POST {:fn '=validate}}
            "_search"   {GET  {:fn '=search}}
            [:id] {:-> ['->resource-exists! '->check-deleted!]
                   :<- ['<-last-modified-header '<-location-header]
                   GET       {:fn '=read}
                   DELETE    {:fn '=delete}
                   PUT       {:-> ['->valid-input! '->has-content-location! '->has-latest-version!]
                              :fn '=update}
                   "_history" {GET {:fn '=history}
                               [:vid] {GET {:fn '=hread}}}}}})

(defn match [meth path]
  (rm/match [meth path] routes))

(match :get "/")

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

(defn dispatch [{handler :handler route :route :as req}]
  (let [filters     (collect :-> route)
        trans       (collect :<- route)]
    (handler (update-in req [:params] merge (:params route)))))

(def app (-> dispatch
             (resolve-handler)
             (resolve-route)
             (ch/site)
             (rmf/wrap-file "resources/public")))

(defn start-server [] (jetty/run-jetty #'app {:port 3000 :join? false}))

(defn stop-server [server] (.stop server))
