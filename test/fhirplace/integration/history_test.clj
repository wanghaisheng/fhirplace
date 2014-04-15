(ns fhirplace.integration.web-test
  (:use midje.sweet)
  (:require [ring.util.request :as request]
            [ring.util.response :as response]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [fhirplace.test-helper :refer :all]))
(use 'plumbing.core)

(def-test-cases test-case
  {:pt-str      (fnk [] (fixture-str "patient"))
   :post        (fnk [pt-str] (POST "/Patient" pt-str))

   :version-loc (fnk [post] (response/get-header post "Location"))
   :history-loc (fnk [version-loc]
                     (string/replace version-loc #"_history/.+" "_history"))
   :resource-loc (fnk [version-loc]
                      (string/replace version-loc #"/_history/.+" ""))

   :pt-json (fnk [] (fixture "patient"))
   :updated-pt (fnk [pt-json] (json/write-str
                               (update-in pt-json [:telecom] conj
                                          {:system "phone"
                                           :value "+919191282"
                                           :use "home"})))
   :put (fnk [resource-loc version-loc updated-pt]
             (PUT resource-loc updated-pt {"Content-Location"
                                           version-loc}))
   :lmd (fnk [put] (response/get-header put "Last-Modified"))
   :lmd-encoded (fnk [lmd] (ring.util.codec/url-encode lmd))

   :history (fnk [history-loc] (GET history-loc))
   :history-with-count (fnk [history-loc] (GET history-loc {"_count" 1}))
   :history-with-since (fnk [history-loc lmd]
                            (GET history-loc {"_since" lmd}))})

(deffacts "History"
  (def res (test-case {}))
  (fact
   "with no params"
   (GET (:history-loc res)) => (every-checker
                                (json-contains [:resourceType] "Bundle")
                                (status? 200)))

  (fact "version-aware put"
        (:put res) => (status? 200))
  (fact "history"
        (:history res) => (count-in-body [:entry] 2))
  (fact "history with _count"
        (:history-with-count res) => (count-in-body [:entry] 1))
  (fact "history with _since"
        (:history-with-since res) => (count-in-body [:entry] 1)))
