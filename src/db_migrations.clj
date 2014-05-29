(ns db-migrations
  (:require [fhirplace.db :as db]
            [clj-sql-up.create  :as create]
            [clj-sql-up.migrate :as migrate]))


(def db db/db)

(defn migrate []
  (migrate/migrate db))

(defn create-migration [args]
  (create/create args))

(defn rollback [args]
  (migrate/rollback db (first args)))

(defn -main [& args]
  (if (empty? args)
    (println "
             Commands:
             create name      Create migration  (eg: migrations/20130712101745082-<name>.clj)
             migrate          Run all pending migrations
             rollback n       Rollback last n migrations  (n defaults to 1)""
             ")
    (let [[command & args] args]
      (cond
        (= command "create")   (create-migration args)
        (= command "migrate")  (migrate)
        (= command "rollback") (rollback args)
        :else (println "No such command: " command)))))

(rollback nil)
(migrate)
#_(migrate)
#_(create-migration ["offer_statuses"])
