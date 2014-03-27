(ns fhirplace.test-helper
  (:require [fhirplace.repositories.resource :refer :all]
            [clojure.test :refer :all]
            [fhirplace.system :as sys]
            [ring.mock.request :as mock]
            [clojure.string :as str]
            [midje.sweet :refer :all]
            [clojure.data.json :as json]))

(def test-system (sys/create :test))

(def test-db (:db test-system))

(defn request [& args]
  ((:handler test-system) (apply mock/request args)))

(defn GET [& args]
  (apply request :get args))

(defn POST [& args]
  (apply request :post args))

(defn PUT [& args]
  (apply request :put args))

(defn DELETE [& args]
  (apply request :delete args))

(defmacro deffacts [str & body]
  (let [smbl (symbol (str/replace str #"[^a-zA-Z]" "_"))]
    `(deftest ~smbl
       (facts ~str
         ~@body))))

(defn fixture-str [name]
  "Returns fixture content as string."
  (slurp (str "test/fixtures/" name ".json")))

(defn fixture [name]
  "Returns fixture content as Clojure data structure (parsed from JSON)."
  (json/read-str (fixture-str name) :key-fn keyword))
