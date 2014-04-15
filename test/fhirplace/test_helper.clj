(ns fhirplace.test-helper
  (:require [fhirplace.repositories.resource :refer :all]
            [clojure.test :refer :all]
            [fhirplace.system :as sys]
            [ring.mock.request :as mock]
            [plumbing.graph :as graph ]
            [schema.core :as s]
            [ring.util.response :as response]
            [clojure.string :as string]
            [midje.sweet :refer :all]
            [clojure.data.json :as json]))

(use 'plumbing.core)

(def test-system (sys/create :test))

(def test-db (:db test-system))

(defn request [& args]
  ((:handler test-system) (apply mock/request args)))

(defn GET [& args]
  (apply request :get args))

(defn POST [& args]
  (apply request :post args))

(defn PUT
  "simpulate PUT request"
  ([uri body]
   (PUT uri body {}))
  ([uri body headers]
   ((:handler test-system)
    (update-in (mock/request :put uri body)
               [:headers] merge headers))))

(defn DELETE [& args]
  (apply request :delete args))

(defn make-uuid [] (str (java.util.UUID/randomUUID)))

(defmacro deffacts [str & body]
  (let [smbl (symbol (string/replace str #"[^a-zA-Z]" "_"))]
    `(deftest ~smbl
       (facts ~str
              ~@body))))

(defn fixture-str [name]
  "Returns fixture content as string."
  (slurp (str "test/fixtures/" name ".json")))

(defn fixture [name]
  "Returns fixture content as Clojure data structure (parsed from JSON)."
  (json/read-str (fixture-str name) :key-fn keyword))

(defn json-body [{body :body :as req}]
  (if body
    (json/read-str body :key-fn keyword)
    (throw (Exception. (str "Could not read body from request (it's empty): "  req)))))

(def get-header response/get-header)

(defmacro def-test-cases [mn m]
  `(def ~mn (graph/lazy-compile ~m)))

(defchecker status? [exp]
  (checker [act]
           (= (:status act) exp)))

(defchecker json-contains [path sample]
  (checker
    [act]
    (let [json (json-body act)
          testable (get-in json path)]
      (if (= testable sample)
        true
        (do
          (println sample " is not matched with " testable)
          false)))))
(defchecker count-in-body [path exp-count]
  (checker
    [act]
    (let [json (json-body act)
          testable (get-in json path)
          testable-count (count testable)]
      (if (= testable-count exp-count)
        true
        (do
          (println "expected count " exp-count " but " testable-count " for " testable)
          false)))))

(defchecker header? [nm regx]
  (checker [act]
           (if (re-find regx (get-header act nm))
             true
             (do
               (println "expected " (get-header act nm) " ~ " regx)
               false))))
