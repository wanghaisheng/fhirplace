(ns fhirplace.resources.convert
  (:require
    [fhirplace.resources.meta :as meta]
    [clojure.xml :as xml]
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.data.json :as json]))

(declare mk-node)

(defn match-polimorph-keys [key attr-name]
  (let [prefix (string/replace (name key) #"\[x\]$" "")
        key-re  (re-pattern (str "^" prefix))]
    (not (nil? (re-find key-re (name attr-name))))))

(defn polymorph-key? [name]
  (not (nil? (re-find #"\[x\]$" (str name)))))

(defn get-tag-name
  "calculate attribute name
  handle [x] attributes"
  [json name]
  (let [kw (keyword name)]
    (cond
      (contains? json kw) kw
      (polymorph-key? name) (first
                              (filter
                                #(match-polimorph-keys name %)
                                (keys json)))
      :else nil)))

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
        (filter (complement nil?)
                (map f seq))))

(defn- mk-child
  "function used in reduce
  corner cases of FHIR logic is here"
  [tag-name path data {name :name :as info}] {:post [(vector? %)]}
  (map-no-nils
    (fn [d]
      (if (= name "contained") ;; specail case if contained
        {:tag :contained
         :content [(mk-node (:resourceType d) (next-path path info d) d)]}
        (mk-node tag-name (next-path path info d) d)))
    (norm-vec data)))


(defn- mk-children
  "build children xml nodes in right order
  going trou meta & building data"
  [path json]
  (reduce (fn [acc {name :name :as info}]
            (let [tag-name (get-tag-name json name)
                  data (get json tag-name)]
              (if (not (nil? data))
                (into acc (mk-child tag-name path data info))
                acc)))
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
