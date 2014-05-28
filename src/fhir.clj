(ns fhir
  (:require [clojure.string :as string]
            [clojure.xml :as cx]
            [clojure.java.io :as cji]))

(import 'java.io.File)

(import 'org.hl7.fhir.instance.formats.JsonParser)
(import 'org.hl7.fhir.instance.formats.XmlParser)
(import 'org.hl7.fhir.instance.formats.JsonComposer)
(import 'org.hl7.fhir.instance.formats.XmlComposer)


(defn- str->input-stream [s]
  (java.io.ByteArrayInputStream. (.getBytes s)))

(defn from-xml [x]
  (.parse (XmlParser.) (str->input-stream x)))

(defn from-json [x]
  (.parse (JsonParser.) (str->input-stream x)))

(defn to-json [res]
  (let [out (java.io.ByteArrayOutputStream.)]
    (.compose (JsonComposer.) out res true)
    (.toString out)))

(defn to-xml [res]
  (let [out (java.io.ByteArrayOutputStream.)]
    (.compose (XmlComposer.) out res true)
    (.toString out)))
