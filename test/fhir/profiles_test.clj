(ns fhir.profiles-test
  (:use midje.sweet)
  (:require
    [fhir.conv :as fc]
    [fhir.profiles :as fp]
    [clojure.java.io :as io]))

(fact conformance-test
      (.toString (.getResourceType fp/conformance))
      => "Conformance")

(first (.getRest fp/conformance))
