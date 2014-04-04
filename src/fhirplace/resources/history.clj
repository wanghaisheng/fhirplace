(ns fhirplace.resources.history
  (require [fhirplace.util :as util]))

(defn entry-title
  [{{resource-type :resourceType} :json id :id version-id :version-id}] 
  (str "Resource of type " resource-type
       ", with id = " id
       " and version-id = "  version-id))

(def last-updated-date (comp :last-modified-date first))

(defn build-entry 
  [{:keys [json last-modified-date id state version-id] :as entry} system]
  (let [history-url (util/cons-url system (:resourceType json) id version-id)
        res-url (util/cons-url system (:resourceType json) id)
        result {:title (entry-title entry)
                :link [{:rel "self" :href history-url}]
                :id res-url
                :content json}]
    (cond
      (= state "deleted") (assoc result :deleted last-modified-date)
      :else (assoc result :updated last-modified-date))))


;; `link' property not added, because don't know where to get it
(defn build-history [entries system]
  {:resourceType "Bundle"
   :title "History of Resource"
   :updated (last-updated-date entries)
   :totalResults (count entries)
   :entry (map #(build-entry % system) entries)})
