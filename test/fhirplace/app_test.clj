(ns fhirplace.app-test
  (:use midje.sweet)
  (:require [fhirplace.app :as app]
            [fhir :as f]
            [clojure.test :refer :all]
            [fhirplace.test-helper :as ft]))


(def parse-tags-mv (app/->parse-tags! identity))
((deftest parse-tags-test
  (let [req {:headers {"category" "dog; label=\"label\"; scheme=\"scheme\""}}
        wrong-req {:headers {"category" "wrong; format=???=...."}}
        resp (parse-tags-mv req)
        null-resp (parse-tags-mv {})
        wrong-resp (parse-tags-mv wrong-req)]

    (is (not (empty? (:tags resp))))
    (is (empty? (:tags null-resp)))
    (is (empty? (:tags wrong-resp)))
    )))
