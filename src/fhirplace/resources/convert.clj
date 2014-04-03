(ns fhirplace.resources.convert
  (:require
    [fhirplace.resources.meta :as meta]
    [clojure.xml :as xml]
    [clojure.java.io :as io]
    [clojure.data.json :as json]))

(declare mk-node)

(defn next-path
  "make switch on complex types"
  [path {nm :name tp :type} {res-type :resourceType}]
  (cond
    res-type res-type
    (and tp (meta/is-complex? tp)) tp
    :else (meta/path->cons path nm)))

(defn mk-children
  "build children xml nodes in right order"
  [path json]
  (let [chld-meta (meta/elem-children path)
        normalize-vec #(if (vector? %) % [%])
        reducer (fn [acc {name :name :as info}]
                  (if-let [data (get json (keyword name))]
                    (if (= name "contained")
                      (conj acc {:tag :contained
                                 :content (filter identity
                                                  (map #(mk-node (:resourceType %) (next-path path info %) %)
                                                       (normalize-vec data)))
                                 })
                      (into acc (filter identity (map #(mk-node name (next-path path info %) %)
                                                      (normalize-vec data)))))
                    acc
                    ))]
    (reduce reducer [] chld-meta)))

(defn mk-node [tag-name path v]
  (cond
    (= (keyword tag-name) :text) nil ; [{:tag :text :content (xml/parse v)}]
    (map? v) {:tag tag-name :attrs {:id (:id v)} :content (mk-children path v)}
    :else {:tag tag-name :attrs {:value (str v)}}))

(defn json->xml* [json]
  (let [res-type (:resourceType json)]
    (with-out-str
      (xml/emit
        {:tag (keyword res-type)
         :attrs {:xmlns "http://hl7.org/fhir"}
         :content (mk-children res-type json)}))))

(defn json->xml
  "convert json string to xml string"
  [json-str]
  (-> json-str
      (json/read-str :key-fn keyword)
      json->xml*))
