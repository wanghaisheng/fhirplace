(ns fhirplace.resources.conversion
  (:require
    [fhirplace.resources.meta :as meta]
    [clojure.xml :as xml]
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.data.json :as json]))

(def ^{:private true} norm-vec #(if (vector? %) % [%]))

(defn- map-no-nils [f seq]
  (into [] (filter (complement nil?) (map f seq))))

(defn- mk-tag
  "helper function"
  ([tag attrs-or-content]
   (cond
     (map? attrs-or-content)  {:tag tag :attrs attrs-or-content}
     (vector? attrs-or-content)  {:tag tag :content attrs-or-content}))
  ([tag attrs content]
   {:tag tag :attrs attrs :content content}))


(defn get-tag-name
  "calculate attribute name
  handle [x] attributes"
  [json name]
  (let [kw (keyword name)]
    (cond
      ;; if json has such key return it
      (contains? json kw) kw
      ;; if polymorphic - find matching
      (meta/polymorphic-attr? name) (first
                                      (filter
                                        #(meta/polymorphic-keys-match? name %)
                                        (keys json)))
      :else nil)))

(defn- next-path
  "make switch on complex types"
  [path {nm :name tp :type} {res-type :resourceType}]
  (cond
    ;; if contained resource
    res-type res-type
    ;; if complex type switch path
    (and tp (meta/is-complex? tp)) tp
    ;; else just concat
    :else (meta/join path nm)))


(defn- fix-id [id]
  (string/replace id #"^#" ""))

(defn- node-attrs [json]
  (if-let [id (:id json)]
    {:id (fix-id id)}
    {}))

(defn- to-inp-stream [s]
  (java.io.ByteArrayInputStream.
    (.getBytes s)))

(defn- add-xhtml-ns [m]
  (assoc m :attrs {:xmlns "http://www.w3.org/1999/xhtml"}))

(defn- mk-text-node
  "handle corner case for text (Narrative)"
  [{status :status div :div :as json}]
  (when div
    (mk-tag :text
            [(mk-tag :status {:value status})
             (-> (str div)
                 to-inp-stream
                 xml/parse
                 add-xhtml-ns)])))

(declare mk-children)

(defn- mk-node [tag-name path v]
  (cond
    ;; text is corner case
    (= (keyword tag-name) :text) (mk-text-node v)
    ;; nested compound element
    (map? v) (mk-tag tag-name (node-attrs v) (mk-children path v) )
    ;; value node
    :else    (mk-tag tag-name {:value (str v)})))

(defn- mk-child
  "function used in reduce
  corner cases of FHIR logic is here"
  [tag-name path data {name :name :as info}] {:post [(vector? %)]}
  (map-no-nils
    (fn [d]
      (if (= name "contained") ;; specail case if contained
        (mk-tag :contained
                [(mk-node (:resourceType d) (next-path path info d) d)])
        (mk-node tag-name (next-path path info d) d)))
    (norm-vec data)))

(defn check-lost-keys!
  "guard from loosing some attributes"
  [json res-xml]
  (let [skip-keys #{:resourceType :id :text}
        extra-keys (filter
                     #(not (contains? (into (set (map :tag res-xml)) skip-keys) %))
                     (keys json))]
    (and (not-empty extra-keys)
         (throw (Exception. (str "There are extra keys: " extra-keys))))
    res-xml))

(defn- mk-children
  "build children xml nodes in right order
  going trou meta & building data"
  [path json]
  (check-lost-keys!
    json
    (reduce (fn [acc {name :name :as info}]
              (let [tag-name (get-tag-name json name)
                    data (get json tag-name)]
                (if (not (nil? data))
                  (into acc (mk-child tag-name path data info))
                  acc)))
            []
            (meta/elem-children path))))

;; Problems
;;
;; * text attribute
;; * contained resources
;; * fix contained resource id
;; * polymorphic attributes [x]
;; * TODO: name references

(defn- mk-root-node [json]
  (let [res-type (:resourceType json)
        res (mk-tag res-type {:xmlns "http://hl7.org/fhir"} (mk-children res-type json))]
    (with-out-str (xml/emit res))))

(defn json->xml
  "convert json string to xml string"
  [json-str]
  (-> json-str
      (json/read-str :key-fn keyword)
      mk-root-node))

(defn xml->json
  "Converts XML string with FHIR resource into JSON representation"
  [xml-str])
