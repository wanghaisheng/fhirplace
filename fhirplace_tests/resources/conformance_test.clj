(ns fhirplace.resources.conformance-test
  (:use midje.sweet)
  (:require [fhirplace.resources.conformance :as c]))

(let [resources ["Patient" "Alert" "AdverseReaction"]
      system-info {:version "01"}
      conf (c/build-conformance resources system-info)]

  (fact "build-conformance returns instance of Conformance resource"
    (:resourceType conf) => "Conformance")

  (fact "Conformance resource reports version of FHIRPlace"
    (:version (:software conf)) => (:version system-info))

  (fact "Conformance describes all available resources in :rest attribute"
    (map :type (get-in conf [:rest 0 :resources])) => resources ))
