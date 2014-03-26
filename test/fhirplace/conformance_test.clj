(ns fhirplace.conformance-test
  (:use midje.sweet)
  (:require [fhirplace.conformance :as c]
            [fhirplace.core :as core]))

(let [resources ["Patient" "Alert" "AdverseReaction"]
      conf (c/build-conformance resources)]

  (fact "build-conformance returns instance of Conformance resource"
    (:resourceType conf) => "Conformance")

  (fact "Conformance resource reports version of FHIRPlace"
    (:version (:software conf)) => (:version core/project))

  (fact "Conformance describes all available resources in :rest attribute"
    (map :type (get-in conf [:rest 0 :resources])) => resources ))
