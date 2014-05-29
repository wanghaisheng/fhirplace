(ns fhir.bundle-test
  (:require
    [midje.sweet :refer :all]
    [fhir.conv :as fc]
    [fhir.bundle :as fb]))

(defn slurp-res [pth]
  (fc/from-xml (slurp pth)))

(def b (fb/bundle
         {:resourceType "Bundle"
          :title "Search result"
          :updated "2012-09-20T12:04:45.6787909+00:00"
          :id "urn:uuid:50ea3e5e-b6a7-4f55-956c-caef491bbc08"
          :entry [{:title "blah"
                   :id "blah"
                   :content (slurp-res "test/fixtures/patient.xml")}]}))

(fact
  "bundle"
  (fc/to-xml (fc/from-xml (fc/to-xml b))) =not=> nil)
