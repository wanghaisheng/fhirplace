(ns fhirplace.interactions.validations-test
  (:require [fhirplace.interactions.validations :refer :all]
            [ring.util.response :refer :all]
            [fhirplace.repositories.resource :as repo]
            [fhirplace.test-helper :refer :all]
            [ring.mock.request :as mock]
            [midje.sweet :refer :all]))

(defn mk-message [body-str]
  {:request (assoc (mock/request :get "some_uri") :body-str body-str)
   :response {}})

(facts "About `parse-json'"
  (parse-json (mk-message "{\"valid_json\": true}")) =not=> (contains {:response {:status 400}})
  (parse-json (mk-message "Not valid json at all!")) => (contains {:response {:status 400}}))

(facts "About `check-existence'"
  (check-existence 
    (assoc-in {} [:request :params :id] ..existed-id..)) =not=> (contains {:response {:status 405}})
  (provided
    (repo/exists? anything ..existed-id..) => true)

  (check-existence 
    (assoc-in {} [:request :params :id] ..non-existed-id..)) => (contains {:response {:status 405}})
  (provided
    (repo/exists? anything ..non-existed-id..) => false))

(facts "About `update-resource'"

  (update-resource (mk-message "")) =not=> (contains {:response {:status 422}})
  (provided
    (repo/update anything anything anything) => nil)

  (update-resource (mk-message "")) => (contains {:response {:status 422}})
  (provided
    (repo/update anything anything anything) =throws=> (java.sql.SQLException. "BOOM!"))

  (facts "Headers"
    (let [res (:response (update-resource (mk-message "")))]
      (fact "Last-Modified should not be nill"
        (get-header res "Last-Modified") =not=> nil)

      (fact "Location should not be nill"
        (get-header res "Location") =not=> nil)

      (fact "Content-Location should not be nill"
        (get-header res "Content-Location") =not=> nil)

      (fact "Status should be 200 OK"
        (:status res) => 200))

    (against-background
      (repo/update anything anything anything) => nil)))
 
(facts "About `check-type'. Should set 404 if type is unknown"
  (do
    (check-type (assoc-in {} [:request :params :resource-type] ..known-type..)) =not=> (contains {:response {:status 404}})
    (check-type (assoc-in {} [:request :params :resource-type] ..non-known-type..)) => (contains {:response {:status 404}})
    (provided
      (repo/resource-types anything) => [..known-type..])))

