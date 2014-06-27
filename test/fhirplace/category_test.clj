(ns fhirplace.category-test
  (:require [fhirplace.category :as subj]
            [clojure.test :refer :all]))

(def samples
  {"dog" [{:term "dog"}]
   "dog,cat" [{:term "dog"} {:term "cat"}]
   "dog; label=\"Canine\"; scheme=\"http://purl.org/net/animals\"" [{:term "dog"
                                                                     :scheme "http://purl.org/net/animals"
                                                                     :label "Canine"}]})

((deftest tags-test
  (doall
    (for [[s r] samples]
      (is (= (subj/parse s) r))))))

