(ns fhirplace.resources.convert
  (:require
    [fhirplace.resources.meta :as meta]
    [clojure.xml :as xml]
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.data.json :as json]))

(declare mk-node)

(defn- next-path
  "make switch on complex types "
  [path {nm :name tp :type} {res-type :resourceType}]
  (cond
    res-type res-type
    (and tp (meta/is-complex? tp)) tp
    :else (meta/join path nm)))

(def ^{:private true} norm-vec #(if (vector? %) % [%]))

(defn- map-no-nils [f seq]
  (into []
        (filter identity
                (map f seq))))

(defn- mk-child
  "function used in reduce
  corner cases of FHIR logic is here"
  [path data {name :name :as info}] {:post [(vector? %)]}
  (map-no-nils
    (fn [d]
      (if (= name "contained") ;; specail case if contained
        {:tag :contained
         :content [(mk-node (:resourceType d) (next-path path info d) d)]}
        (mk-node name (next-path path info d) d)))
    (norm-vec data)))

(defn- mk-children
  "build children xml nodes in right order
  going trou meta & building data"
  [path json]
  (reduce (fn [acc {name :name :as info}]
            (if-let [data (get json (keyword name))]
              (into acc (mk-child path data info))
              acc))
          []
          (meta/elem-children path)))

(defn- fix-id [id]
  (string/replace id #"^#" ""))

(defn- node-attrs [json]
  (if-let [id (:id json)]
    {:id (fix-id id)}
    {}))

(defn- to-inp-stream [s]
  (java.io.ByteArrayInputStream.
    (.getBytes s)))

(defn- mk-text-node
  "handle corner case for text (Narrative)"
  [{status :status div :div :as json}]
  (when div
    {:tag :text
     :content [{:tag :status :attrs {:value status}}
               (assoc
                 (xml/parse (to-inp-stream (str div)))
                 :attrs {:xmlns "http://www.w3.org/1999/xhtml"})]}))

(defn- mk-node [tag-name path v]
  (cond
    ;; TODO: [{:tag :text :content (xml/parse v)}]
    (= (keyword tag-name) :text) (mk-text-node v)

    (map? v) {:tag tag-name
              :attrs (node-attrs v)
              :content (mk-children path v)}

    :else    {:tag tag-name
              :attrs {:value (str v)}}))

(defn- json->xml* [json]
  (let [res-type (:resourceType json)
        res {:tag (keyword res-type)
             :attrs {:xmlns "http://hl7.org/fhir"}
             :content (mk-children res-type json)}]
    (with-out-str (xml/emit res))))

(defn json->xml
  "convert json string to xml string"
  [json-str]
  (-> json-str
      (json/read-str :key-fn keyword)
      json->xml*))
