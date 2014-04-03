(ns fhirplace.resources.convert
  (:require
    [clojure.xml :as xml]
    [clojure.java.io :as io]
    [clojure.data.json :as json]))


(defn mk-node [[tag-name v]]
  (cond
    (= tag-name :text) [] ; [{:tag :text :content (xml/parse v)}]
    (map? v) [{:tag tag-name :content (mapcat mk-node v)}]
    (vector? v) (mapcat #(mk-node [tag-name %]) v)
    :else [{:tag tag-name :attrs {:value (str v)}}]))

(defn json->xml* [m]
  (with-out-str
    (xml/emit
      {:tag (keyword (:resourceType m))
       :attrs {:xmlns "http://hl7.org/fhir"}
       :content (mapcat mk-node m)})))

(defn json->xml
  "convert json string to xml string"
  [json-str]
  (-> json-str
      (json/read-str :key-fn keyword)
      json->xml*))

(defn- to-file
  [path] {:pre [(not (nil? (io/resource path)))]}
  (-> (io/resource path)
      (.toURI)
      (slurp)))

(require '[clojure.zip :as z])
(require '[clojure.data.zip.xml :as dzx])



(import 'java.io.File)
(def resources-meta
  (->
    (io/resource "fhir/profiles-resources.xml")
    (.toURI)
    (File.)
    (xml/parse)
    (z/xml-zip)))

(map z/node (dzx/xml-> resources-meta :entry :content :Profile :structure :type))


