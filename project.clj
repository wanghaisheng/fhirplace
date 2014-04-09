(defproject fhirplace "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :ring {:handler fhirplace.app/app}

  :plugins [[lein-ring "0.8.10"]
            [lein-midje "3.0.0"]]

  :resource-paths ["resources"]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.4"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [org.postgresql/postgresql "9.3-1101-jdbc41"]
                 [ring-mock "0.1.5"]
                 [compojure "1.1.6"]
                 [org.clojure/algo.monads "0.1.5"]
                 [ring "1.2.1"]
                 [cheshire "5.3.1"]
                 [clojure-saxon "0.9.3"]
                 [org.clojure/core.match  "0.2.1"]
                 [honeysql "0.4.3"]
                 [org.clojure/data.zip "0.1.1"]]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [com.cemerick/pomegranate "0.2.0"]
                                  [midje "1.6.0"]
                                  [clj-time "0.6.0"]
                                  [im.chit/vinyasa "0.1.8"]
                                  [io.aviso/pretty "0.1.10"]
                                  [spyscope "0.1.3"]
                                  [criterium "0.4.1"]
                                  [leiningen "2.3.4"]
                                  [org.clojure/java.classpath "0.2.0"]]
                   :plugins [[lein-kibit "0.0.8"]]
                   :injections [(require '[vinyasa.inject :as inj])
                                (inj/inject 'clojure.core
                                            '[[vinyasa.inject inject]
                                              [vinyasa.pull pull]
                                              [vinyasa.lein lein]
                                              [vinyasa.reimport reimport]
                                              [midje.repl load-facts autotest]])

                                (require 'io.aviso.repl
                                         'clojure.repl
                                         'clojure.main)
                                (alter-var-root #'clojure.main/repl-caught
                                                (constantly @#'io.aviso.repl/pretty-pst))
                                (alter-var-root #'clojure.repl/pst
                                                (constantly @#'io.aviso.repl/pretty-pst))] }})
