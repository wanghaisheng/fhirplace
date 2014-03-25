(defproject fhirplace "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :ring {:handler fhirplace.app/app}

  :plugins [[lein-ring "0.8.10"]
            [lein-midje "3.0.0"]]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.4"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [org.postgresql/postgresql "9.3-1101-jdbc41"]
                 [ring-mock "0.1.5"]
                 [compojure "1.1.6"]
                 [ring "1.2.1"]]

  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [com.cemerick/pomegranate "0.2.0"]
                                  [midje "1.6.0"]
                                  ;; [slamhound "1.3.3"]
                                  [im.chit/vinyasa "0.1.8"]
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
                                              [midje.repl load-facts]])]
                   }})
