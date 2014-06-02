(ns fhirplace.resources.meta-test
  (:use midje.sweet)
  (:require
    [fhirplace.resources.meta :as m]))

(def recursive-path '("Questionnaire"
                      "group"
                      "group"
                      "group"
                      "question"
                      "group"
                      "question"
                      "choice"
                      "system"))

(fact
  (m/lookup '("CarePlan" "activity" "goal")) => {:max "*"
                                                 :min "0"
                                                 :nameRef nil
                                                 :type "idref"
                                                 :weight 58}

  (m/lookup '("Foo" "unexistent" "path")) => nil)

(fact
  (m/resolve-path '("Questionnaire" "group" "question" "name" "coding" "system")) => '("Coding" "system")
  (m/resolve-path '("Questionnaire" "group")) => '("Questionnaire" "group"))

(fact
  (m/normalize-path recursive-path) => '("Questionnaire" "group" "question" "choice" "system"))

(fact
  (m/smart-lookup recursive-path) => {:max "1"
                                      :min "0"
                                      :nameRef nil
                                      :type "uri"
                                      :weight 7})
