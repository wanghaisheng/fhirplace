(ns fhir.operation-outcome-test
  (:require
    [midje.sweet :refer :all]
    [fhir.conv :as fc]
    [fhir.operation-outcome :as fo]))

(def o (fo/operation-outcome
         {:text {:status "generated"
                 :div "<div></div>" }
          :issue [{:severity "fatal"
                   :details "Resource cannot be parsed"}
                  {:severity "fatal"
                   :details "Problem one"}]}))

(fact
  "bundle"
  (fc/to-xml (fc/from-xml (fc/to-xml o))) =not=> nil)
