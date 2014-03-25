(ns fhirplace.resources.conformance-test
  (:use midje.sweet
        fhirplace.resources.conformance)
  (:require [fhirplace.resources.conformance :refer :all]))

(defn build-conformance []
  { :messaging [{ :event [{ :mode "sender" :code {:code "admin-notify"} }] }] })

;; (fact "Conformance builder builds Conformance resource"
;;   (:resourceType (build-conformance)) => "Conformance")


;;   (fact " A Conformance statement SHALL have at least one of description, software, or implementation"
;;     (map (fn [k] (count (k conformance))) [:software :implementation :description])))

(fact "Exist validator validates presence of attribute in object"
  (let [foo-key-exist? (v-exist :foo)]
    (foo-key-exist? {} []) => [{:type :exist :key :foo}]
    (foo-key-exist? {:foo 42} []) => []))

(fact "or validator returns all errors from all nested validators"
  (let [or-validator (v-or (v-exist :foo) (v-exist :bar))]
    (or-validator {} []) => [{:errors [{:type :exist :key :foo} {:type :exist :key :bar}] :type :or}]
    (or-validator {:foo 12} []) => []
    (or-validator {:bar 23} []) => []))

(fact "Conformance Resource must satisfy invariant-6"
  (let [conf (build-conformance)
        invconf (update-in conf
                  [:messaging 0 :event] conj
                  {:mode "sender" :code {:code "admin-notify"}})]

    (invariant-6 conf) => truthy
    (invariant-6 invconf) => falsey))


(fact "get-path returns values according to passed path"
  (get-path {:foo [{:bar 42} {:bar 56}]} [:foo :bar]) => [42 56]
  (get-path {:foo [{:bar 42} {:bar 56}]} [:foo]) => [{:bar 42} {:bar 56}]
  (get-path {:foo [{:bar 42 :code "notok"} {:bar 56 :code "ok"}]} [:foo [:bar 56] :code]) => ["ok"]
  (get-path [{:foo [{:bar 42}]} {:foo [{:bar 56}]}] [:foo]) => [[{:bar 42}] [{:bar 56}]]
  (get-path {:event [{:mode "receiver" :code 42} {:mode "sender" :code 56}]} [:event [:mode "sender"] :code]) => [56]
)
