(ns fhirplace.test-helper
  (:require [clojure.test :refer :all]
            [fhirplace.db :as db]))

(defmacro def-db-test  [nm & body]
  `(deftest ~nm
     (db/rollback-transaction ~@body)))

(defn fixture [nm] (slurp (str "test/fhirplace/fixtures/" nm)))
