(ns fhirplace.jsquery-test
  (:use midje.sweet)
  (:require
    [fhirplace.jsquery :as fj]))

(fact "jsquery"
  (fj/jsquery "confirmed") => "\"confirmed\""
  (fj/jsquery 1) => 1

  (fj/jsquery
    [:= "status" "confirmed"])
  => "\"status\" = \"confirmed\""

  (fj/jsquery
    [:= "status" 1])
  => "\"status\" = 1"


  (fj/jsquery
    [:&
     [:= "system" 2]
     [:= "code" 3]])
  => "\"system\" = 2 & \"code\" = 3"

  (fj/jsquery
    [:|
     [:= "system" 2]
     [:= "code" 3]])
  => "\"system\" = 2 | \"code\" = 3"

  (fj/jsquery
    [:|
     [:= "system" 2]
     [:= "code" 3]])
  => "\"system\" = 2 | \"code\" = 3"

  (fj/jsquery
    ["category.coding.#"
     [:&
      [:= "system" 2]
      [:= "code" 3]]])
  => "\"category\".\"coding\".# ( \"system\" = 2 & \"code\" = 3 )"

  (fj/jsquery
    [:&
     [:= "status" "confirmed"]
     ["category.coding.#"
      [:&
       [:= "system" 2]
       [:= "code" 3]]]
     ["code.coding.#"
      [:&
       [:= "system" 2]
       [:= "code" 3]]]])
  =>
  "\"status\" = \"confirmed\" & \"category\".\"coding\".# ( \"system\" = 2 & \"code\" = 3 ) & \"code\".\"coding\".# ( \"system\" = 2 & \"code\" = 3 )"
  )

