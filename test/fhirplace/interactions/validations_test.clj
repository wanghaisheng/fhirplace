(ns fhirplace.interactions.validations-test
  (:require [fhirplace.interactions.validations :refer :all]
            [ring.util.response :refer :all]
            [fhirplace.repositories.resource :as repo]
            [fhirplace.test-helper :refer :all]
            [ring.mock.request :as mock]
            [midje.sweet :refer :all]))

(defn mk-req-with-body [body-str]
  (assoc (mock/request :get "some_uri") :body-str body-str))

(facts "About `parse-json'"
  (parse-json (mk-req-with-body "{\"valid_json\": true}")) =not=> (contains {:status 400})
  (parse-json (mk-req-with-body "Not valid json at all!")) => (contains {:status 400}))

(facts "About `check-existence'"
  (check-existence {:params {:id ..existed-id..}}) =not=> (contains {:status 405})
  (provided
    (repo/exists? anything ..existed-id..) => true)

  (check-existence {:params {:id ..non-existed-id..}}) => (contains {:status 405})
  (provided
    (repo/exists? anything ..non-existed-id..) => false))

(facts "About `update-resource'"
  (update-resource {}) =not=> (contains {:status 422})
  (provided
    (repo/update anything anything anything) => nil)
  
  (update-resource {}) => (contains {:status 422})
  (provided
    (repo/update anything anything anything) =throws=> (java.sql.SQLException. "BOOM!")))

(facts "About `pack-update-result'"
  (let [succ-req (pack-update-result (mk-req-with-body ""))]
    (fact "Last-Modified should not be nill"
      (get-header succ-req "Last-Modified") =not=> nil)

    (fact "Location should not be nill"
      (get-header succ-req "Location") =not=> nil)

    (fact "Content-Location should not be nill"
      (get-header succ-req "Content-Location") =not=> nil)

    (fact "Status should be 200 OK"
      (:status succ-req) => 200)))
