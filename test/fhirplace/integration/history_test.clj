(ns fhirplace.integration.history-test
  (:use midje.sweet)
  (:require [ring.util.request :as request]
            [ring.util.response :as response]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [fhirplace.test-helper :refer :all]))
(use 'plumbing.core)

(def-test-cases test-case
  {:pt-str       (fnk [format] (fixture-str "patient" format))
   :post         (fnk [pt-str format]
                      (POST (str "/Patient?_format=" format) pt-str))

   :version-loc  (fnk [post]
                      (response/get-header post "Location"))
   :history-loc  (fnk [version-loc]
                      (string/replace version-loc #"_history/.+" "_history"))
   :resource-loc (fnk [version-loc]
                      (string/replace version-loc #"/_history/.+" ""))

   :res-history  (fnk [history-loc format]
                     (GET history-loc {:_format format}))

   :pt-json      (fnk [] (fixture "patient"))
   :updated-pt   (fnk [pt-json] (json/write-str
                                 (update-in pt-json [:telecom] conj
                                            {:system "phone"
                                             :value "+919191282"
                                             :use "home"})))
   :put          (fnk [resource-loc version-loc updated-pt]
                      (PUT resource-loc updated-pt {"Content-Location"
                                                    version-loc}))
   :lmd          (fnk [put] (response/get-header put "Last-Modified"))
   :lmd-encoded  (fnk [lmd] (ring.util.codec/url-encode lmd))

   :history      (fnk [history-loc put format]
                      (GET history-loc {:_format format}))
   :history-with-count (fnk [history-loc put format]
                            (GET history-loc {:_count 1
                                              :_format format}))
   :history-with-since (fnk [history-loc put lmd-encoded format]
                            (GET history-loc {:_format format
                                              :_since lmd-encoded}))})

(defn history* [format]
  (def res (test-case {:format format}))
  
  (fact
     "with no params"
     (:res-history res) => (every-checker
                            (body-contains [:resourceType] "Bundle")
                            (status? 200)))

  (fact "version-aware put"
          (:put res) => (status? 200))
  (fact "history"
          (:history res) => (count-in-body [:entry] 2))
  (fact "history with _count"
        (:history-with-count res) => (count-in-body [:entry] 1))
  (fact "history with _since"
          (:history-with-since res) => (count-in-body [:entry] 1)))

(deffacts "History"
  (history* "application/json")
  #_(history* "application/xml"))
