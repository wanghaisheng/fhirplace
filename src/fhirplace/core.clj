(ns fhirplace.core
  (:require [clojure.java.jdbc :as sql]))

(def db-spec
  {:subprotocol "postgresql"
   :subname "//127.0.0.1:5454/fhirbase"
   :user "vagrant"})

(defn resource-types []
  (set
    (map :path
      (sql/query db-spec ["SELECT DISTINCT(path[1]) FROM meta.resource_elements"]))))
