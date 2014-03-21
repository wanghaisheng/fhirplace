(ns fhirplace.handler
  (:use ring.util.response)
  (:require [fhirplace.core :as core]))

(def routes)
(defn find-handler
  "Finds appropriate handler for request using routes table."
  [request]
  (nth (first (filter
            (fn [[method uri-regexp handler]]
              (and
                (= method (:request-method request))
                (re-matches uri-regexp (:uri request))
                handler))
            routes)) 2))

(defn create-handler
  "Handler for CREATE queries."
  [request]
  (-> (response "CREATE")
    (content-type "text/plain")))

(defn update-handler
  "Handler for UPDATE queries."
  [request]
  (-> (response "UPDATE")
    (content-type "text/plain")))

(defn delete-handler
  "Handler for DELETE queries."
  [request]
  (-> (response "DELETE")
    (content-type "text/plain")))

(defn read-handler
  "Handler for READ queries."
  [request]

  (-> (response "READ")
    (content-type "text/plain")))

(def uuid-regexp
  #"[0-f]{8}-([0-f]{4}-){3}[0-f]{12}")

(def resource-types-regexp
  (re-pattern (str "(" (clojure.string/join "|" (core/resource-types)) ")")))

(def routes
  "Main routes table, each route is an HTTP verb + URI regexp + handler name.
Create: POST   [base]/[type] {?_format=[mime-type]}
Search:  GET   [base]?[parameters] {&_format=[mime-type]}
         GET   [base]/[type]?[parameters] {&_format=[mime-type]}
         GET   [base]/[type]/_search?[parameters] {&_format=[mime-type]}
         GET   [base]/[compartment]/[id]/?[parameters]  {&_format=[mime-type]}
Delete: DELETE [base]/[type]/[id]
Update: PUT    [base]/[type]/[id] {?_format=[mime-type]}"

  [[:post   (re-pattern (str "(?i)" "/" resource-types-regexp))                  create-handler]
   [:get    (re-pattern (str "(?i)" "/" resource-types-regexp "/" uuid-regexp )) read-handler]
   [:delete (re-pattern (str "(?i)" "/" resource-types-regexp "/" uuid-regexp )) delete-handler]
   [:put    (re-pattern (str "(?i)" "/" resource-types-regexp "/" uuid-regexp )) update-handler]])

(defn root-handler
  "Generic request handler, dispatches incoming query with `routes` table."
  [request]

  (when-let [handler-func (find-handler request)]
    (handler-func request)))
