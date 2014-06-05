(ns fhirplace.search
  (:require
    [fhir :as f]
    [ring.util.codec :as ruc]
    [clojure.string :as cs]))

(defn xpath->path [x]
  (let [clear-xpath #(cs/replace % #"f:" "")
        parts       (map clear-xpath (cs/split x #"/"))
        tp          (first parts)
        path-parts  (rest parts)
        prof        (f/profile tp)]

    (loop [pth [tp]
           [x xs] path-parts]
      (println pth)
      (if x
        (recur (conj pth x) xs)
        pth))
    #_(cs/join "." path-parts)))



(defn build-for-param [info nm v]
  (println info))

(defn params-to-jsquery [res-type query-str]
  (let [prof         (f/profile res-type)
        query-params (ruc/form-decode query-str)
        params-desc  (group-by :name (get-in prof [:structure 0 :searchParam]))
        res-attrs    (group-by :path (get-in prof [:structure 0 :element]))]

    (for [[k v] query-params]
      (build-for-param (first (get params-desc k)) k v))))

