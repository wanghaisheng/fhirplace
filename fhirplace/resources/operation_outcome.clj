(ns fhirplace.resources.operation-outcome)

(defn- build-issue [issue]
  (when (contains? #{"error" "warning" "information" "fatal"} (:severity issue))
    issue))

(defn exception-with-message [e]
  (let [sw (java.io.StringWriter.)]
       (.printStackTrace e (java.io.PrintWriter. sw))
       ;; (println sw)
       (str sw)))

(defn build-operation-outcome
  ([issues]
     (let [made-issues (remove nil? (map build-issue issues))]
       (when (seq made-issues)
         {:issue made-issues
          :resourceType "OperationOutcome" })))

  ([severity details]
     (build-operation-outcome [{:severity severity :details details}])))
