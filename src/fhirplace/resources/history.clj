(ns fhirplace.resources.history
  (require [fhirplace.util :as util]))

(defn entry-title
  [{{resource-type :resourceType} :json id :id version-id :version_id}] 
  (str "Resource of type " resource-type
       ", with id = " id
       " and version-id = "  version-id))

(def last-updated-date (comp :last_modified_date first))

(defn build-entry 
  [{:keys [json last-modified-date id] vid :version_id :as entry} system]
  (let [history-url (util/cons-url system (:resourceType json) id vid)
        res-url (util/cons-url system (:resourceType json) id)]
    {:title (entry-title entry)
     :link [{:rel "self" :href history-url}]
     :id res-url
     :updated last-modified-date
     :content json}))


;; `link' property not added, because don't know where to get it
(defn build-history [entries system]
  {:resourceType "Bundle"
   :title "History of Resource"
   :updated (last-updated-date entries)
   :totalResults (count entries)
   :entry (map #(build-entry % system) entries)})
