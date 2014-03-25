(ns fhirplace.resources.conformance)

(defn v-exist [k]
  (fn [obj errors]
    (if-not (k obj)
      (conj errors {:type :exist :key k})
      errors)))
