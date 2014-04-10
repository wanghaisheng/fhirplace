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
  [path key1 key2]
  (let [path1 (conj path (name key1))
        path2 (conj path (name key2))
        el1 (meta/smart-lookup (reverse path1))
        el2 (meta/smart-lookup (reverse path2))]
    (compare (:weight el1) (:weight el2))))

(defn convert-json-object-to-xml
  [path value]

  (let [root? (= 1 (count path))
        special-keys (if root?
                       '(:resourceType :text :contained)
                       '())

        keys-to-dissoc (concat special-keys
                         (filter
                           #(.startsWith (name %) "_")
                           (keys value)))

        cleaned-value (apply dissoc value keys-to-dissoc)
        sorted-value (into
                       (sorted-map-by
                         (partial json-attributes-comparator path))
                       cleaned-value)]

    (list ; wrap return value in a list
      (apply
        xml/element
        (keyword (first path))                       ; tag name
        (if root? {:xmlns "http://hl7.org/fhir"} {}) ; tag attrs
        (reduce                                      ; inner xml
          (fn [acc [k v]]
            (if (or (> 1 (count path))
                  (not (contains? #{:resourceType :text :contained} k)))
              (concat acc
                (convert-json-value-to-xml (conj path (name k)) v))
              acc))
          '() sorted-value)))))

(defn convert-json-value-to-xml
  "Recursive function to call on each JSON value. Outputs XML node."
  [path value]
  (println "converting" (reverse path))

  (cond
    (map? value) (convert-json-object-to-xml path value)
    (or (list? value)
      (vector? value)) (convert-json-array-to-xml path value)
      :else (convert-json-data-to-xml path value)))

(defn perform
  "Entry point to perform conversion."
  [json]

  (first (convert-json-value-to-xml (list (:resourceType json)) json)))
