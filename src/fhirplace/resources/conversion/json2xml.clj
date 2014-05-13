(ns fhirplace.resources.conversion.json2xml
  (:use spyscope.core)
  (:require
   [fhirplace.resources.meta :as meta]
   [fhirplace.util :as util]
   [clojure.data.xml :as xml]
   [clj-time.format :as c-format]
   [clj-time.coerce :as c-coerce]))

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

(defn convert-json-text-attr-to-xml
  [path value]
  (let [text (:text value)]
    (if (and text (seq text))
      (list
        (xml/element :text {}
          (xml/element :status {:value (:status text)})
          (xml/element :div {:xmlns "http://www.w3.org/1999/xhtml"}
            (first
              (:content
               (xml/parse (java.io.StringReader. (:div text))))))))
      '())))

(declare perform)
(defn convert-json-containeds-attr-to-xml
  [path value]

  (if (:contained value)
    (map
      (fn [res-xml] (xml/element :contained {} res-xml))
      (map perform (:contained value)))
    '()))

(declare root?)
(defn cleanup-json-value
  "Before converting JSON object (map) to XML we must
   remove some attribtes (resourceType, text, contained)"
  [value path]

  (let [is-root (root? path)       ; do we converting resource root?
        special-keys (if is-root   ; when we converting resource root,
                                   ; we want to ignore those special keys
                       '(:resourceType :text :contained)
                       '())

        keys-to-dissoc (concat special-keys ; also we want to discard
                         (filter            ; keys starting with '_'
                           #(.startsWith (name %) "_")
                           (keys value)))]

    ; discard keys
    (apply dissoc value keys-to-dissoc)))

(defn sort-json-attributes
  "Before converting JSON object to XML we must arrange
   it's attributes in right order (using :weight value found in meta)."
  [value path]

  (let [get-weight (fn [key]
                     (:weight
                      (meta/smart-lookup
                        (reverse
                          (conj path (name key))))))

        comparator (fn [key1 key2]
                     (compare (get-weight key1) (get-weight key2)))]

    (into (sorted-map-by comparator) value)))

(defn convert-special-attributes-to-xml
  [path value]
  (if (root? path)
    (concat
      (convert-json-text-attr-to-xml path value)
      (convert-json-containeds-attr-to-xml path value))
    '()))

(defn get-xml-attributes-for-tag
  [path value]

  (if (root? path)
    (util/discard-nils
      {:xmlns "http://hl7.org/fhir"
       :id (:_id value)})
    {}))

(defn- root?
  [path]
  (= 1 (count path)))

(defn convert-json-object-to-xml
  [path value]

  (let [is-root (root? path)
        sorted-value (-> value
                       (cleanup-json-value path)
                       (sort-json-attributes path))]

    (list ; wrap return value in a list
      (apply
        xml/element
        (keyword (first path))                       ; tag name
        (get-xml-attributes-for-tag path value)      ; tag attrs
        (concat                                      ; inner xml
          (convert-special-attributes-to-xml path value)
          (reduce ; recursively convert every child attr
            (fn [acc [k v]]
              (concat acc
                (convert-json-value-to-xml (conj path (name k)) v)))
            '() sorted-value))))))

(defn convert-json-value-to-xml
  "Recursive function to call on each JSON value. Outputs XML node."
  [path value]
  (cond
    (map? value) (convert-json-object-to-xml path value)
    (sequential? value) (convert-json-array-to-xml path value)
      :else (convert-json-data-to-xml path value)))

(defn perform
  "Entry point to perform conversion."
  [json]

  (first (convert-json-value-to-xml (list (:resourceType json)) json)))

(defn b-el [attr & els]
  (apply xml/element attr {} els))

(defn b-at [attr json]
  (b-el attr (str (attr json))))

(defn b-dt [attr json]
  (b-el attr
        (c-format/unparse (c-format/formatters :date-time) (c-coerce/from-date (attr json)))))

(defn b-en [attr json]
  (map (fn [e] (b-el attr
                     (map (fn [k]
                            (cond (some #{k} '(:title :id))
                                  (b-at k e)
                                  (= k :updated)
                                  (b-dt k e)))
                          (keys e))))
       (attr json)))

(defn bundle
  "Converts bundle to xml"
  [json]
  (xml/emit-str (xml/element :feed {:xmlns "http://www.w3.org/2005/Atom"}
                             (map (fn [e]
                                    (cond (some #{e} '(:title :id))
                                          (b-at e json)
                                          (= e :updated)
                                          (b-dt e json)
                                          (= e :entry)
                                          (b-en e #spy/p json)))
                                  (keys json)))))
