(ns fhirplace.resources.conversion.json2xml
  (:require
   [fhirplace.resources.meta :as meta]
   [clojure.data.xml :as xml]))

(defn convert-json-data-to-xml
  [path value]

  (list                                 ; wrap return value into a list
    (xml/element (keyword (first path))
      {:value (str value)})))

(declare convert-json-value-to-xml)
(defn convert-json-array-to-xml
  [path value]

  ;; cause `convert-json-value-to-xml' may return more than one element
  ;; we use reduce to accumulate them in flat list
  (reduce
    (fn [acc item]
      (concat acc (convert-json-value-to-xml path item)))
    '() value))

(defn json-attributes-comparator
  "Comparator for sorting resource attrs by it's weight what means
   that elements will appear in XML in right order."
  [path key1 key2]

  (let [path1 (conj path (name key1))
        path2 (conj path (name key2))
        el1 (meta/smart-lookup (reverse path1))
        el2 (meta/smart-lookup (reverse path2))]
    (compare (:weight el1) (:weight el2))))

(defn convert-json-text-attr-to-xml
  [path value]
  (let [text (:text value)]
    (if (and text (not (empty? text)))
      (list
        (xml/element :text {}
          (xml/element :status {:value (:status text)})
          (xml/element :div {:xmlns "http://www.w3.org/1999/xhtml"}
            (first
              (:content
               (xml/parse (java.io.StringReader. (:div text)))))))
        )
      '())))

(defn convert-json-containeds-attr-to-xml
  [path value]

  (if (:contained value)
    (map
      (fn [res-xml] (xml/element :contained {} res-xml))
      (map perform (:contained value)))
    '()))

(defn convert-json-object-to-xml
  [path value]

  (let [root? (= 1 (count path)) ; do we converting resource root?
        special-keys (if root?   ; when we converting resource root,
                                        ; we want to ignore those special keys
                       '(:resourceType :text :contained)
                       '())

        keys-to-dissoc (concat special-keys ; also we want to discard
                         (filter            ; keys starting with '_'
                           #(.startsWith (name %) "_")
                           (keys value)))

        cleaned-value (apply dissoc value keys-to-dissoc) ; discard keys
        sorted-value (into                                ; sort map
                       (sorted-map-by
                         (partial json-attributes-comparator path))
                       cleaned-value)]

    (list ; wrap return value in a list
      (apply
        xml/element
        (keyword (first path))                       ; tag name
        (if root? {:xmlns "http://hl7.org/fhir"} {}) ; tag attrs
        (concat                                      ; inner xml
          (if root?    ; convert contained and text on root node
            (concat
              (convert-json-text-attr-to-xml path value)
              (convert-json-containeds-attr-to-xml path value))
            '())

          (reduce                                    ; convert value
            (fn [acc [k v]]
              (concat acc
                (convert-json-value-to-xml (conj path (name k)) v)))
            '() sorted-value))))))

(defn convert-json-value-to-xml
  "Recursive function to call on each JSON value. Outputs XML node."
  [path value]
  (cond
    (map? value) (convert-json-object-to-xml path value)
    (or (list? value)
      (vector? value)) (convert-json-array-to-xml path value)
      :else (convert-json-data-to-xml path value)))

(defn perform
  "Entry point to perform conversion."
  [json]

  (first (convert-json-value-to-xml (list (:resourceType json)) json)))
