(ns fhirplace.integration.web-test
  (:use midje.sweet)
  (:require [ring.util.request :as request]
            [ring.util.response :as response]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [fhirplace.test-helper :refer :all]))


(def patient-json-str (fixture-str "patient"))
(def patient-json (fixture "patient"))

(deffacts "FHIRPlace respond to /info with debug info"
  (fact
    (GET "/info") =not=> nil))


(comment
  (deffacts "About READing non-existent resource"
    (let [response (GET (str "/patient/" (make-uuid)))]
      (:status response) => 404))

  (deffacts "About HISTORY"
    (let [create-response (POST "/Patient" patient-json-str)
          resource-loc-with-history (response/get-header create-response "Location")
          resource-loc (string/replace resource-loc-with-history #"_history/.+" "_history")
          resource-loc-simple (string/replace resource-loc-with-history #"/_history/.+" "")]

      (fact "get history"
            (GET resource-loc) => (contains {:status 200})
            (json-body (GET resource-loc)) => (contains {:resourceType "Bundle"
                                                         :entry anything}))

      (fact "get history with _count and _since"
            (let [update-body (json/write-str
                                (update-in  patient-json [:telecom] conj
                                           {:system "phone"
                                            :value "+919191282"
                                            :use "home"} ))
                  update-response (PUT-LONG resource-loc-simple update-body {"Content-Location" resource-loc-with-history})
                  update-last-modified (response/get-header update-response "Last-Modified")]
              (:status update-response) => 200
              (count (:entry (json-body (GET resource-loc)))) => 2
              (count (:entry (json-body (GET (str resource-loc "?_count=1"))))) => 1
              (count (:entry (json-body (GET (str resource-loc (str "?_since=" (ring.util.codec/url-encode update-last-modified))))))) => 1))))

  )
