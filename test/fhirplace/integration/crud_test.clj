(ns fhirplace.integration.crud-test
  (:use midje.sweet)
  (:require [ring.util.request :as request]
            [ring.util.response :as response]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.test :refer :all]
            [plumbing.graph :as graph ]
            [schema.core :as s]
            [fhirplace.test-helper :refer :all]))

(use 'plumbing.core)

(defn json-body [{body :body :as req}]
  (if body
    (json/read-str body :key-fn keyword)
    (throw (Exception. (str "Could not read body from request (it's empty): "  req)))))

(defn header [resp name]
  (response/get-header resp name))

(def test-cmp
  (graph/lazy-compile
    {:pt-str       (fnk [] (fixture-str "patient"))
     :pt           (fnk [] (fixture "patient"))

     :create-pt    (fnk [pt-str]
                        (POST "/Patient" pt-str))

     :pt-loc       (fnk [create-pt]
                        (header create-pt "Location"))

     :pt-uri       (fnk [pt-loc]
                        (first
                          (clojure.string/split pt-loc #"/_history/")))

     :read-pt     (fnk [pt-uri]
                       (GET pt-uri))

     :vread-pt    (fnk [pt-loc]
                       (GET pt-loc))

     :new-telecom (fnk []
                       {:system "phone" :value "+919191282" :use "home"})

     :new-pt      (fnk [pt new-telecom]
                       (update-in  pt [:telecom] conj new-telecom))

     :new-pt-json      (fnk [new-pt] (json/write-str new-pt))

     :wrong-chg   (fnk [new-pt-json pt-uri]
                       (PUT pt-uri new-pt-json))

     :chg-pt      (fnk [pt-loc pt-uri new-pt-json]
                       (PUT pt-uri new-pt-json {"Content-Location" pt-loc}))

     :dup-chg-pt  (fnk [pt-loc pt-uri new-pt-json chg-pt]
                       (PUT pt-uri new-pt-json {"Content-Location" pt-loc}))

     :chg->read-pt (fnk [chg-pt] (GET (header chg-pt "Location")))

     :del-pt      (fnk [pt-uri] (DELETE pt-uri))

     :del-2-pt    (fnk [pt-uri del-pt] (DELETE pt-uri))

     :del->read    (fnk [pt-uri del-pt] (GET pt-uri)) }))

(defchecker status? [exp]
  (checker [act]
           (= (:status act) exp)))

(defchecker json-contains [path sample]
  (checker [act]
           (let [json (json-body act)
                 testable (get-in json path)]
             (= testable sample))))

(defchecker header? [nm regx]
  (checker [act]
           (re-find regx (header act nm))))

(deftest integration-test

  (def res (test-cmp {}))
  (facts
    "create"
    (:create-pt res) => (contains {:status 201})
    (:pt-loc res) => #"/Patient/.+/_history/.+")

  (facts "read"
         (:read-pt res)
         => (every-checker
              (status? 200)
              (header? "Content-Location" #"/Patient/.+/_history/.+")
              (header? "Last-Modified" #"....-..-.. .+")
              (contains {:body (complement nil?)})
              (json-contains [:name] (:name (:pt res)))))

  (facts "vread"
         (:vread-pt res)
         => (every-checker
              (status? 200)
              (contains {:body (complement nil?)})
              (json-contains [:name] (:name (:pt res)))))

  (facts
    "when UPDATEing without specified version error with outcome"
    (:wrong-chg res) => (status? 412)
    (get-in (json-body (:wrong-chg res)) [:issue 0 :details]) => #"Version id is missing"
    (get-in (json-body (:wrong-chg res)) [:resourceType]) => "OperationOutcome")

  (facts "empty udpate body"
         (:chg-pt res) => (status? 200)
         (:body (:chg-pt res)) => "")

  (facts "updated pt"
         (:telecom (json-body (:chg->read-pt res))) => (contains [(:new-telecom res)]))

  (fact "when UPDATEing with specified previous resource version"
        (:dup-chg-pt res) => (status? 409))

  (facts "delete"
         (:del-pt res)     => (status? 204)
         (:del-2-pt res)   => (status? 204)
         (:del->read res)  => (status? 410)))
