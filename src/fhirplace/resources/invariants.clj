(ns fhirplace.resources.invariants)

(defn- normalize-vector [v]
  (if (vector? v) v [v]))

(defn- filter-nodes [nds [k v]]
  (filterv #(= (k %) v) nds))

(defn- append-to-nodes [acc v]
  (vec ((if (vector? v) concat conj) acc v)))

(defn- apply-filter [flt nodes]
  (let [nds (normalize-vector nodes)]
    (cond
      (keyword? flt) (reduce #(append-to-nodes %1 (flt %2)) [] nds )
      (vector? flt)  (filter-nodes nds flt)
      :else          (throw (Exception. "unknown filter")))))

(defn- apply-last-filter [flt nodes]
  (let [nds (normalize-vector nodes)]
    (cond
      (keyword? flt) (filterv identity (mapv flt nds))
      (vector? flt)  (filter-nodes nds flt)
      :else          (throw (Exception. "unknown filter")))))

(defn- filter-with-path [obj filters]
  (->> (reduce #(apply-filter %2 %1) obj (butlast filters))
       (apply-last-filter (last filters))))

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
