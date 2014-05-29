(ns fhirplace.routes
  (:require [route-map :as rm]))

(def routes
  {"metadata" {:GET {:fn '=metadata}}
   [:type] {:-> ['->type-supported!]
            :<- ['<-format '<-outcome-on-exception]
            :POST       {:-> ['->valid-input!] :fn '=create}
            "_validate" {:POST {:fn '=validate}}
            "_search"   {:GET  {:fn '=search}}
            [:id] {:-> ['->resource-exists! '->check-deleted!]
                   :<- ['<-last-modified-header '<-location-header]
                   :GET       {:fn '=read}
                   :DELETE    {:fn '=delete}
                   :PUT       {:-> ['->valid-input! '->has-content-location! '->has-latest-version!]
                               :fn '=update}
                   "_history" {:GET {:fn '=history}
                               [:vid] {:GET {:fn '=hread}}}}}})

(defn match [meth path]
  (rm/match [meth path] routes))

(defn collect [k match]
  (apply into
         (filter (complement nil?)
                 (map k (:parents match)))))

(collect :-> (match :get "encounter/1/_history/2"))
(collect :<- (match :get "encounter/1/_history/2"))

[(:match (match :get "encounter/1"))
 (:match (match :get "encounter/_search"))]
