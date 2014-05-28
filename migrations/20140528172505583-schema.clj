;; migrations/20140528172505583-schema.clj
(require ['clojure.java.jdbc :as 'jdbc])

(defn up []
  [(jdbc/create-table-ddl
     :resources
     [:version_id :uuid "PRIMARY KEY"]
     [:logical_id :uuid "NOT NULL"]
     [:resource_type :varchar "NOT NULL"]
     [:last_modified_date "TIMESTAMP WITH TIME ZONE" "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]
     [:data :jsonb "NOT NULL"])])

(defn down []
  ["DROP TABLE resources"])
