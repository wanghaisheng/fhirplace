(ns fhirplace.resources.history
  (require [fhirplace.util :as util]
           [clj-time.coerce :as tc]))

(defn- entry-title
  [{{resource-type :resourceType} :json id :id version-id :version-id}]
  (str "Resource of type " resource-type
       ", with id = " id
       " and version-id = "  version-id))

(defn build-entry
  [{:keys [json last_modified_date id state version-id] :as entry} system]
  (let [history-url (util/cons-url system (:resourceType json) id version-id)
        res-url (util/cons-url system (:resourceType json) id)
        result {:title (entry-title entry)
                :link [{:rel "self" :href history-url}]
                :id res-url
                :published (java.util.Date.)
                :content json}]
    (cond
     (= state "deleted") (assoc result :deleted last_modified_date)
     :else (assoc result :updated last_modified_date))))


;; `link' property not added, because don't know where to get it
(defn build-history [entries system]
  {:resourceType "Bundle"
   :title "History of Resource" ; content aware title
   :id "2323"
   :author {:name "Fhirplace Server by HealthSamurai."
            :uri "http://healthsamurai.github.io"}
   :link [;;{:rel "self" :href uri}
          {:rel "fhir-base" :href (util/cons-url system)}]
   :updated (java.util.Date.)
   :totalResults (count entries)
   :entry (map #(build-entry % system) entries)})
