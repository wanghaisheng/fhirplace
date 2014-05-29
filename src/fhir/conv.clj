(ns fhir.conv
  (:require [clojure.string :as string]
            [clojure.xml :as cx]
            [clojure.java.io :as cji]))

(import 'java.io.File)

(import 'org.hl7.fhir.instance.formats.JsonParser)
(import 'org.hl7.fhir.instance.formats.XmlParser)
(import 'org.hl7.fhir.instance.formats.JsonComposer)
(import 'org.hl7.fhir.instance.formats.XmlComposer)

(import 'org.hl7.fhir.instance.model.AtomFeed)

(defn- str->input-stream [s]
  (java.io.ByteArrayInputStream. (.getBytes s)))


(defn return-item [x]
  (or (.getResource x) (.getFeed x)))

(defn from-xml [x]
  (return-item (.parseGeneral (XmlParser.) (str->input-stream x))))

(defn from-json [x]
  (return-item (.parseGeneral (JsonParser.) (str->input-stream x))))

(defn to-json [res]
  (let [out (java.io.ByteArrayOutputStream.)]
    (.compose (JsonComposer.) out res true)
    (.toString out)))

(defn to-xml [res]
  (let [out (java.io.ByteArrayOutputStream.)]
    (.compose (XmlComposer.) out res true)
    (.toString out)))
