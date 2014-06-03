(comment (ns user
  "Namespace to support hacking at the REPL."
  (:require [fhirplace.core :as fc]
            [clojure.tools.namespace.repl :as ns-repl]
            [clojure.tools.namespace.move :refer :all]
            [clojure.repl :refer :all]
            [clojure.pprint :refer :all]
            [clojure.java.io :as cjio]
            [clojure.string :as str]
            [clojure.java.classpath :as cjc]
            [criterium.core :as crit]))

(defn reset []
  (ns-repl/refresh)))
