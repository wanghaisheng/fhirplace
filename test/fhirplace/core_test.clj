(ns fhirplace.core-test
  (:use midje.sweet)
  (:require [fhirplace.core :as fc]))

(defn mw1 [h]
  (fn [r]
    (h (merge r {:a 1 :last 1}))))

(defn mw2 [h]
  (fn [r]
    (h (merge r {:b 2 :last 2} ))))

(fact
  "Build stack wrap all midlewares"
  ((fc/build-stack
     identity
     [mw1 mw2]) {}) => {:a 1 :b 2 :last 2})

