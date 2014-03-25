(ns fhirplace.db)

(defn conn []
  {:subprotocol "postgresql"
   :subname "//127.0.0.1:5432/fhirbase"
   :user "postgres"})
