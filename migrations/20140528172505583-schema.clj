;; migrations/20140528172505583-schema.clj
(require ['clojure.java.jdbc :as 'jdbc])
(require '[fhir.profiles :as fp])


(def tables
  (for [x (fp/profiles)]
    (let [nm (-> x (.getResource) (.getStructure) (first) (.getType) (.getValue))]
      (.toLowerCase nm))))

(defn up []
  (concat
    (for [tbl tables]
      (jdbc/create-table-ddl
        (str "\"" tbl "\"")
        [:version_id :uuid "PRIMARY KEY"]
        [:logical_id :uuid "NOT NULL"]
        [:status :varchar "NOT NULL" "DEFAULT 'active'"]
        [:resource_type :varchar "NOT NULL"]
        [:last_modified_date "TIMESTAMP WITH TIME ZONE" "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]
        [:published  "TIMESTAMP WITH TIME ZONE" "NOT NULL"]
        [:data :jsonb "NOT NULL"]))
    (for [tbl tables]
      (jdbc/create-table-ddl
        (str "" tbl "_history")
        [:version_id :uuid "PRIMARY KEY"]
        [:logical_id :uuid "NOT NULL"]
        [:status :varchar "NOT NULL" "DEFAULT 'active'"]
        [:resource_type :varchar "NOT NULL"]
        [:last_modified_date "TIMESTAMP WITH TIME ZONE" "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]
        [:published  "TIMESTAMP WITH TIME ZONE" "NOT NULL"]
        [:data :jsonb "NOT NULL"]))
    [(jdbc/create-table-ddl
      :tags
      [:id :uuid "PRIMARY KEY"]
      [:resource_id :uuid "NOT NULL"]
      [:term :varchar "NOT NULL"]
      [:label :varchar "NOT NULL"]
      [:scheme :varchar "NOT NULL"])]))

(defn down []
  (concat ["DROP TABLE tags"]
        (for [tbl tables] (str "DROP TABLE \"" tbl "\" CASCADE"))
        #_(for [tbl tables] (str "DROP TABLE \"" tbl "_history\" CASCADE"))))
