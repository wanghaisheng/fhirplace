(ns fhirplace.conformance
  (:require [clj-time.core :as t]))

(defn- build-resource [resource]
  {:type resource
   :readHistory false
   :updateCreate true
   :searchInclude []
   :operation (mapv (fn [i] {:type i})
                ["read" "update" "create" "delete" "validate" "search-type"])})

(defn- build-rest [resources]
  [{:mode "server"
    :documentation "Blah"
    :security {:service [{ :text "HTTP Digest" }]
               :descrition "Use plain simple HTTP auth"}
    :resources (mapv build-resource resources)}])

(defn build-conformance
  "Returns Conformance resource describing this FHIRPlace server"
  [resources]
  {:resourceType "Conformance"
   :name "PHR Template"
   :publisher "Health Samurai Developers"
   :telecom [{:system "url" :value "http://healthsamurai.github.io"}]
   :description "Blah blah blah"
   :date (t/local-date 2013 3 20)
   :fhirVersion "DSTU"
   :acceptUnknown true
   :format ["json" "xml"]
   :rest (build-rest resources)})

