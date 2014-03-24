(defproject fhirplace "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :ring {:handler fhirplace.app/app}

  :plugins [[lein-ring "0.8.10"]
            [lein-midje "3.0.0"]]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.2.2"]
                 [org.clojure/data.json "0.2.4"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [org.postgresql/postgresql "9.3-1101-jdbc41"]]


  :profiles {:dev {:dependencies [[midje "1.6.0"]]}})
