(ns fhirplace.resources.operation-outcome)

(defn- build-issue [issue]
  (when (contains? #{"error" "warning" "information" "fatal"} (:severity issue))
    issue))

(defn build-operation-outcome
  ([issues]
     (let [made-issues (remove nil? (map build-issue issues))]
       (when-not (empty? made-issues)
         {:issue made-issues
          :resourceType "OperationOutcome" })))

  ([severity details]
     (build-operation-outcome [{:severity severity :details details}])))

