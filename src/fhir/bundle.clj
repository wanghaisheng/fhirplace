(ns fhir.bundle
  (:require
    [fhir.conv :as fc]
    [cheshire.generate :as cg]
    [cheshire.core :as cc]))

(import 'org.hl7.fhir.instance.model.Resource)

(cg/add-encoder
  Resource
  (fn [c jsonGenerator]
    (.writeRawValue jsonGenerator (fc/to-json c))))

(defn bundle [attrs]
  "build bundle from hash-map
  with entry :content parsed to fhir.model.Resource"
  (let [b (cc/generate-string attrs)]
    (fc/from-json b)))
