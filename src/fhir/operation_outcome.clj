(ns fhir.operation-outcome
  (:require
    [fhir.conv :as fc]
    [cheshire.generate :as cg]
    [schema.core :as s]
    [plumbing.core :as p :include-macros true]
    [cheshire.core :as cc])
  (:import org.hl7.fhir.instance.model.OperationOutcome))


(def OperationOutcomeSchema
  {:text {:status (s/enum "generated")
          :div s/Str}
   :issue [{:severity (s/enum "fatal" "error" "warning" "information")
            :details s/Str}]})

(defn operation-outcome
  "build outcome from hash-map"
  [attrs]
  {:pre [(s/validate OperationOutcomeSchema attrs)]
   :post [(instance? OperationOutcome %)]}

  (let [b (cc/generate-string (merge attrs {:resourceType "OperationOutcome"}))]
    (fc/from-json b)))
