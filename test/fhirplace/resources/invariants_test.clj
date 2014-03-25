(ns fhirplace.resources.invariants-test
  (:use midje.sweet
        fhirplace.resources.conformance)
  (:require [fhirplace.resources.invariants :refer :all]))

(defn build-conformance []
  { :messaging [{ :event [{ :mode "sender" :code {:code "admin-notify"} }] }] })


(fact "get-path returns values according to passed path"
      (xpath {:a 1} [:a])
      => [1]

      (xpath {:a 1} [:b])
      => []

      (xpath
        [{:foo [{:bar 42}]}
         {:foo [{:bar 56}]}]
        [:foo])
      => [[{:bar 42}] [{:bar 56}]]

      (xpath
        {:foo [{:bar 42}
               {:bar 56}]}
        [:foo :bar])
      => [42 56]

      (xpath
        {:foo [{:bar 42 :code "notok"}
               {:bar 56 :code "ok"}]}
        [:foo [:bar 56] :code])
      => ["ok"]

      (xpath
        {:foo [{:bar 42} {:bar 56}]}
        [:foo])
      => [[{:bar 42} {:bar 56}]]

      (xpath
        {:event [{:mode "receiver" :code 42}
                 {:mode "sender" :code 56}]}
        [:event [:mode "sender"] :code])
      => [56]
      )

(def-invariant inv-1
  "inv-1"
  (not (empty? (xpath [:rest]))))

(fact
  (inv-1 {:rest "ups"}) => true
  (inv-1 {:not-rest "ups"}) => false)

(def-invariant inv-2
  "inv-2"
  (= (count (xpath [:document [:mode "producer"]]))
     (count (distinct (xpath [:document [:mode "producer"] :profile])))))

(fact
  (let [obj {:document
             [{:mode "producer" :profile {:reference "url1"}}
              {:mode "producer" :profile {:reference "url1"}}]}]
    (inv-2 obj) => false
    (inv-2 (update-in obj [:document 1 :profile] #(assoc % :reference "other-url"))) => true))
