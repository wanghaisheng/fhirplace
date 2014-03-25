(ns fhirplace.db)

(defn conn []
  {:subprotocol "postgresql"
   :subname "//127.0.0.1:5433/fhirbase"
   :user "vagrant"})
