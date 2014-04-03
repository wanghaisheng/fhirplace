(ns fhirplace.resources.convert
  (:require
    [fhirplace.resources.meta :as meta]
    [clojure.xml :as xml]
    [clojure.string :as string]
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

(def ^{:private true} norm-vec #(if (vector? %) % [%]))

(defn- map-no-nils [f seq]
  (into []
        (filter identity
                (map f seq))))

(defn mk-child
  "function used in reduce
  dirty FHIR logic is here"
  [path data {name :name :as info}]
  {:post [(vector? %)]}
  (map-no-nils
    (fn [d]
      (if (= name "contained") ;; specail case if contained
        {:tag :contained
         :content [(mk-node (:resourceType d) (next-path path info d) d)]}
        (mk-node name (next-path path info d) d)))
    (norm-vec data)))

(defn mk-children
  "build children xml nodes in right order"
  [path json]
  (reduce (fn [acc {name :name :as info}]
            (if-let [data (get json (keyword name))]
              (into acc (mk-child path data info))
              acc))
          []
          (meta/elem-children path)))

(defn- fix-id [id]
  (string/replace id #"^#" ""))

(defn node-attrs [json]
  (if-let [id (:id json)]
    {:id (fix-id id)}
    {}))

(defn mk-node [tag-name path v]
  (cond
    (= (keyword tag-name) :text) nil ; [{:tag :text :content (xml/parse v)}]

    (map? v) {:tag tag-name
              :attrs (node-attrs v)
              :content (mk-children path v)}

    :else    {:tag tag-name
              :attrs {:value (str v)}}))

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
