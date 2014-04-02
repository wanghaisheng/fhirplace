(ns fhirplace.resources.history)

(defn resource-hist-url [entry] "Dummy url")
(defn resource-url [entry] "Dummy url")

(defn entry-title
  [{{resource-type :resourceType} :json id :id version-id :version_id}] 
  (str "Resource of type " resource-type
       ", with id = " id
       " and version-id = "  version-id))

(def last-updated-date (comp :last_modified_date first))

(defn build-entry 
  [{:keys [json last-modified-date] :as entry}]
  {:title (entry-title entry)
   :link [{:rel "self" :href (resource-hist-url entry)}]
   :id (resource-url entry)
   :updated last-modified-date
   :content json})


(defn build-history [entries url]
  {:resourceType "Bundle"
   :title "History of Resource"
   :updated (last-updated-date entries)
   :link [{:rel "self" :href url}]
   :totalResults (count entries)
   :entry (map build-entry entries)})
