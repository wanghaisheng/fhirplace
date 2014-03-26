(ns fhirplace.conformance-test
  (:use midje.sweet)
  (:require [fhirplace.conformance :as c]))

(let [resources ["Patient" "Alert" "AdverseReaction"]
      conf (c/build-conformance resources)]

  (fact "Conformance describes all available resources in :rest attribute"
    (map :type (get-in conf [:rest 0 :resources])) => resources ))
