(ns fhirplace.util)

(defn discard-nils [m]
  (reduce (fn [acc [k v]]
            (cond
              (map? v) (assoc acc k (discard-nils v))
              (sequential? v) (assoc acc k
                                     (mapv (fn [x]
                                             (if (coll? x)
                                               (discard-nils x)
                                               x))
                                           v))
              (nil? v) acc
              :else (assoc acc k v)))
          {} m))

(defn discard-indexes [m]
  (reduce (fn [acc [k v]]
            (cond
              (map? v) (assoc acc k (discard-indexes v))
              (sequential? v) (assoc acc k
                                     (mapv (fn [x]
                                             (if (coll? x)
                                               (discard-indexes x)
                                               x))
                                           v))
              (= :_index k) acc
              :else (assoc acc k v)))
          {} m))
