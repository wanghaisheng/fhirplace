(ns fhirplace.interactions.resource-test
  (:require [fhirplace.interactions.resource :as res]
            [ring.util.response :refer :all]
            [fhirplace.repositories.resource :as repo]
            [fhirplace.test-helper :refer :all]
            [ring.mock.request :as mock]
            [midje.sweet :refer :all]))

(defn mk-message [body-str]
  (assoc (mock/request :get "some_uri") :body-str body-str))

(facts "About `parse-json'"
  (let [parse-json (res/wrap-with-json identity)]
    (parse-json (mk-message "{\"valid_json\": true}")) =not=> (contains {:status 400})
    (parse-json (mk-message "Not valid json at all!")) => (contains {:status 400})))

(facts "About `check-existence'"
  (let [check-existence (res/wrap-resource-not-exist identity 405)]
    (check-existence {:params {:id ..existed-id..}}) =not=> (contains {:status 405})
    (provided
      (repo/exists? anything ..existed-id..) => true)

    (check-existence {:params {:id ..non-existed-id..}}) => (contains {:status 405})
    (provided
      (repo/exists? anything ..non-existed-id..) => false)))

(facts "About `check-type'. Should set 404 if type is unknown"
  (let [check-type (res/wrap-with-check-type identity)]
    (check-type {:params {:resource-type ..known-type..}}) =not=> (contains {:status 404})
    (check-type {:params {:resource-type ..not-known-type..}}) => (contains {:status 404})
    (provided
      (repo/resource-types anything) => [..known-type..])))

