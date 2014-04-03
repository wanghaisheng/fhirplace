(ns ^{:doc "meta information about FHIR resources
           all queries go here"}
  fhirplace.resources.meta
  (:require
    [clojure.string :as string]
    [clojure.xml :as xml]
    [clojure.zip :as zip]
    [clojure.data.zip.xml :as xx]
    [clojure.java.io :as io]))

(import 'java.io.File)


(defn to-nodes
  [coll]
  (map zip/node coll))

(defn xml->nodes [& args]
  (to-nodes (apply xx/xml-> args)))

(defn attr
  "get attr"
  [n attr]
  (get-in n [:attrs attr]))

(defn mattr
  "construct get attr function"
  [attr]
  (fn [n] (attr n attr)))

(defn zattr
  "construct get attr function"
  [loc a]
  (attr (zip/node loc) a))

(defn xml->val [loc & path]
  (if-let [node (apply xx/xml1-> loc path)]
    (zattr node :value)))

(defn resource
  "return zipper loc"
  [db res-type]
  (xx/xml1-> db :entry :content :Profile :structure
             #(= (xml->val % :type) res-type)))



(defprotocol FHIRPath
  (child? [p1 p2]))


(extend-type String
  FHIRPath
  (child? [p c]
    (let [p (string/split p #"\.")
          c (string/split c #"\.")]
      (= p (butlast c)))))

(defn path->cons [path next]
  (str path "." (name next)))

(defn path->leaf [path]
  (last (string/split path #"\.")))

(defn path->resource [path]
  (first (string/split path #"\.")))

(defn mk-elem [loc]
  (let [path (xml->val loc :path)]
    {:path path
     :name (path->leaf path)
     :min (xml->val loc :definition :min)
     :max (xml->val loc :definition :max)
     :type (xml->val loc :definition :type :code) }))

(defn elem-children* [res-loc path]
  (xx/xml-> res-loc :element
            #(child? path (xml->val % :path))))

(defn load-profile [path]
  (->
    (io/resource path)
    (.toURI)
    (File.)
    (xml/parse)
    (zip/xml-zip)))

(def res-profile
  (load-profile "fhir/profiles-resources.xml"))

(def dt-profile
  (load-profile "fhir/profiles-types.xml"))

(defn is-complex? [type-name]
  (Character/isUpperCase (first type-name)))

(defn elem-children
  [path]
  (let [res-type (path->resource path)
        res-loc  (or (resource res-profile res-type)
                     (resource dt-profile res-type))]
    (if res-loc
      (map
        mk-elem
        (elem-children* res-loc path))
      (throw (Exception. (str "could not find meta for " path))))))
