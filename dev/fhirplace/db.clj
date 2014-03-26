(ns fhirplace.db)

(defn conn []
  {:subprotocol "postgresql"
   :subname "//127.0.0.1:5454/fhirbase"
   :user "vagrant"})
