(ns fhirplace.handler-test
  (:use midje.sweet)
  (:require [fhirplace.handler :refer :all]))

(defn mk-request [method uri]
  {:request-method method
   :uri uri})

(fact "Requests to CREATE action are delegated to `create-handler`"
  (find-handler (mk-request :get "/patient"))  =>     nil
  (find-handler (mk-request :post "/patient")) =>     create-handler)

(fact "Requests to READ action are delegated to `read-handler`"
  (find-handler (mk-request :get "/patient/51400fa3-c860-4e44-baaa-13db5c8d8621")) => read-handler
  (find-handler (mk-request :get "/group/51400fa3-c860-4e44-baaa-13db5c8d8621")) => read-handler
  (find-handler (mk-request :post "/group/51400fa3-c860-4e44-baaa-13db5c8d8621")) => nil)

(fact "Requests to DELETE action are delegated to `delete-handler`"
  (find-handler (mk-request :delete "/group/51400fa3-c860-4e44-baaa-13db5c8d8621")) => delete-handler)

(fact "Requests to UPDATE action are delegated to `update-handler`"
  (find-handler (mk-request :put "/group/51400fa3-c860-4e44-baaa-13db5c8d8621")) => update-handler)
