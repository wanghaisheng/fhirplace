(ns fhirplace.resources.conformance)

(defn v-exist [k]
  (fn [obj errors]
    (if-not (k obj)
      (conj errors {:type :exist :key k})
      errors)))

(defn v-count [k]
  (fn [obj errors]
    (if-not (k obj)
      (conj errors {:type :count :key k})
      errors)))

(defn v-or [& fncts]
  (fn [obj errors]
    (let [result (map (fn [f] (f obj [])) fncts)]
      (if (some empty? result)
        []
        (conj errors {:type :or :errors (apply concat result)})))))

(defn validator [obj]
  (let [inv-1 (v-or (v-exist :rest) (v-exist :messaging) (v-exist :document))]
    (inv-1 obj [])))

(defn invariant-6
  "On Conformance.messaging: The set of events per messaging endpoint
 must be unique by the combination of code & mode"
  [conformance]

  (let [events (mapcat :event (:messaging conformance))
        filter-by-mode (fn [evts mode] (filter (fn [e] (= (:mode e) mode)) events))
        sender-events (filter-by-mode events "sender")
        receiver-events (filter-by-mode events "receiver")
        get-codes (fn [events] (map (fn [e] (:code (:code e))) events))]

    (and
      (= (count sender-events) (count (distinct (get-codes sender-events))))
      (= (count receiver-events) (count (distinct (get-codes receiver-events)))))))

(defn invariant-7
  "The set of documents must be unique by the combination of profile & mode"
  [conformance]
  (let [documents (mapcat :document ())])
  )

(defn get-path* [obj path acc])

(defn get-path [obj path]
  (get-path* obj path []))

(defn get-prop-by-path-element [obj path]
  (if (vector? obj)
    (if (vector? path)
      (let [k (first path)
            value (last path)]
        (filter (fn [i] (= (k i) value)) obj))
      (map path obj))

    (if (vector? path)
      (let [k (first path)
            value (last path)]
        (if (= (k obj) value) obj nil))

      (path obj))))

(defn get-path* [obj path acc]
  (let [path-head (first path)
        path-rest (next path)
        next-obj  (get-prop-by-path-element obj path-head)]

    (if path-rest
      (reduce
        (fn [a item] (if-let [h (get-path* item path-rest a)] (conj a h)))
        acc
        next-obj)
      next-obj)))
