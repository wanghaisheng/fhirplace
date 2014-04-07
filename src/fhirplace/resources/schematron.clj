(ns fhirplace.resources.schematron
  (:require
    [clojure.java.io :as io]
    [saxon :as xml]
    [fhirplace.util :as util]))

(def ^{:private true} iso-svrl-xsl
  (delay
    (xml/compile-xslt
      (util/resource-to-file "schematron/iso_svrl_for_xslt2.xsl"))))

(def ^{:private true} name-spaces
  {:svrl "http://purl.oclc.org/dsdl/svrl"})

(defn- extract-error [nd]
  {:trace (xml/query "distinct-values(./@location)" nd)
   :message (xml/query "./svrl:text/string()" name-spaces nd) })

(defn parse-errors [result]
  (let [failed-asserts (xml/query "//svrl:failed-assert" name-spaces result)
        errors (mapv extract-error failed-asserts)]
    (when-not (empty? errors) errors)))

(defn compile-sch
  "return schematron validation function
  which get string and return schematron result xml"
  [path]
  (let [sch (-> (util/resource-to-file path)
                (xml/compile-xml)
                (@iso-svrl-xsl)
                (xml/compile-xslt))]
    (fn [doc-str]
      (-> (xml/compile-xml doc-str)
          (sch)
          (parse-errors)))))
