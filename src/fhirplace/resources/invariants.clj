(ns fhirplace.resources.invariants)

(defn- normalize-vector [v]
  (if (vector? v) v [v]))

(defn- filter-nodes
  "filter maps by attribute"
  [nds [k v]]
  (filterv #(= (k %) v) nds))

(defn- append-to-nodes
  "make nodes set: if v is vector concat else conj"
  [acc v last?]
  (if (nil? v)
    []
    (let  [op (if (and (vector? v) (not last?)) concat conj)]
      (vec (op acc v)))))

;TODO: remove copy-paste
(defn- apply-filter
  "filter in path"
  [flt nodes & last?]
  (let [nds (normalize-vector nodes)]
    (cond
      (keyword? flt) (reduce #(append-to-nodes %1 (flt %2) last?) [] nds )
      (vector? flt)  (filter-nodes nds flt)
      :else          (throw (Exception. "unknown filter")))))

(defn- filter-with-path [obj filters]
  (let [nodes (reduce #(apply-filter %2 %1) obj (butlast filters))]
    (apply-filter (last filters) nodes true)))

(def ^{:dynamic true} *obj*)

(defn xpath
  ([path] (filter-with-path *obj* path))
  ([obj path] (filter-with-path obj path)))

(defmacro def-invariant
  "def-invariant [var-name doc-string & forms]"
  [vr doc-str & forms]
  `(defn ~vr [obj#]
     ~doc-str
     (binding [*obj* obj#] ~@forms)))
