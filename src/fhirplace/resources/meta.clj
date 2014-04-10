(ns fhirplace.resources.meta
  (:require [clojure.string :as string]
            [saxon :as xml]))

(def fhir-elements (xml/compile-xml
                     (java.io.File. "resources/xml2json/fhir-elements.xml")))

(defn- convert-xml-nodes-to-map
  [nodes]
  (reduce
    (fn [acc node]
      (let [name (-> node
                   .getNodeName
                   .toString
                   keyword)
            value (.getAttributeValue node (net.sf.saxon.s9api.QName. "value"))]
        (assoc acc name value)))
    {} nodes))

(defn lookup
  "Returns metainfo describing FHIR element."
  [path]
  (let [path-string (string/join "." path)
        element (xml/query
                  (str "/elements/element[@path='" path-string "']")
                  fhir-elements)]

    (if element
      (let [nodes (xml/query "*" element)
            values (convert-xml-nodes-to-map nodes)]
        (if (contains? values :weight)
          (assoc values :weight (read-string (:weight values)))
          values))
      nil)))

(declare normalize-path)
(defn- normalize-path*
  [path-in path-out]

  (if (empty? path-in)
    path-out

    (let [el (lookup path-out)
          name-ref (and el (:nameRef el))]
      (if (and name-ref (not (empty? name-ref)))
        (normalize-path* path-in (reverse (into '() (string/split name-ref #"\."))))
        (normalize-path* (pop path-in) (concat path-out (list (first path-in))))))))

(defn normalize-path
  "Normalizes path using nameRefs in fhir-elements.xml. So
   Questionnaire.group.group.group.question.group.question.text
   becomes
   Questionnaire.group.question.text"
  [path]
  (normalize-path* path '()))

(declare resolve-path)
(defn- resolve-path*
  [path path-tail]

  (if (empty? path)
    (throw (Exception. "meta/resolve-path called with empty path")))

  (let [el (lookup path)]
    (if el
      ; we have an element
      (let [rawtype (:type el)
            eltype (if (and rawtype (.startsWith rawtype "Resource("))
                     "ResourceReference"
                     rawtype)]
        (cond
          (and eltype (empty? path-tail)) path
          (and eltype (not (empty? path-tail))) (resolve-path* (conj path-tail eltype) '())
          :else path))

      ; we don't have an element
      (resolve-path* (butlast path) (conj path-tail (last path))))))

(defn resolve-path
  "Resolves path using complex types, so
   Questionnaire.group.question.name.coding becomes
   CodeableConcept.coding"
  [path]
  (resolve-path* path '()))

(defn smart-lookup
  [path]
  (-> path
    normalize-path
    resolve-path
    lookup))
