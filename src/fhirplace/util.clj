(ns fhirplace.util)

(defn discard-nils [m]
  (reduce (fn [acc [k v]]
            (cond
              (map? v) (assoc acc k (clean-map v))
              (vector? v) (assoc acc k (mapv clean-map v))
              (nil? v) acc
              :else (assoc acc k v)))
          {} m))
