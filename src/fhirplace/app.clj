(ns fhirplace.app
  (:use ring.util.response
        ring.util.request)
  (:require [compojure.core :as cc]
            [compojure.route :as cr]
            [compojure.handler :as ch]
            [clojure.string :as cs]
            [fhir :as f]
            [fhirplace.category :as fc]
            [fhir.operation-outcome :as fo]
            [fhirplace.db :as db]
            [ring.adapter.jetty :as jetty]
            [clojure.data.json :as json]
            [environ.core :as env]))

(import 'org.hl7.fhir.instance.model.Resource)
(import 'org.hl7.fhir.instance.model.AtomFeed)

(defn url [& parts]
  (str
    (env/env :fhirplace-web-url)
    "/"
    (apply str (interpose "/" parts))))

(defn- determine-format
  "Determines request format (:xml or :json)."
  [{{fmt :_format} :params}]
  (or (get {"application/json" :json
            "application/xml"  :xml} fmt)
      :json))

(defn <-format [h]
  "formatting midle-ware
  expected body is instance of fhir reference impl"
  (fn [req]
    (let [{bd :body :as resp} (h req)
          fmt (determine-format req)]
      ;; TODO set right headers
      (println "Formating: " bd)
      (if (and bd (or (instance? Resource bd) (instance? AtomFeed bd)))
        (assoc resp :body (f/serialize fmt bd))
        resp))))

(defn- get-stack-trace [e]
  (let [sw (java.io.StringWriter.)]
    (.printStackTrace e (java.io.PrintWriter. sw))
    (println "ERROR: " sw)
    (str sw)))

(defn- outcome [status text & issues]
  {:status status
   :body (fo/operation-outcome
           {:text {:status "generated" :div (str "<div>" text "</div>")}
            :issue issues })})

(defn <-outcome-on-exception [h]
  (fn [req]
    (println "<-outcome-on-exception")
    (try
      (h req)
      (catch Exception e
        (println "Exception")
        (println (get-stack-trace e))
        (outcome 500 "Server error"
                 {:severity "fatal"
                  :details (str "Unexpected server error " (get-stack-trace e))})))))


(defn ->type-supported! [h]
  (fn [{{tp :type} :params :as req}]
    (println "TODO: ->type-supported!")
    (if tp
      (h req)
      (outcome 404 "Resource type not supported"
               {:severity "fatal"
                :details (str "Resource type [" tp "] isn't supported")}))))

(defn ->resource-exists! [h]
  (fn [{{tp :type id :id } :params :as req}]
    (println "->resource-exists!")
    (if (db/-resource-exists? tp id)
      (h req)
      (outcome 404 "Resource not exists"
               {:severity "fatal"
                :details (str "Resource with id: " id " not exists")}))))

;; TODO: move to fhir f/errors could do it
(defn- safe-parse [x]
  (try
    [:ok (f/parse x)]
    (catch Exception e
      [:error (str "Resource could not be parsed: \n" x "\n" e)])))

(defn ->parse-tags!
  "parse body and put result as :data"
  [h]
  (fn [req]
    (println "->parse-tags!")
    (if-let [c (get-in req [:headers "category"])]
      (h (assoc req :tags (fc/safe-parse c)))
      (h (assoc req :tags [])))))

(defn ->parse-body!
  "parse body and put result as :data"
  [h]
  (fn [{bd :body :as req}]
    (println "->parse-body!")
    (let [[st res] (safe-parse (slurp bd)) ]
      (if (= st :ok)
        (h (assoc req :data res))
        (outcome 400 "Resource could not be parsed"
                 {:severity "fatal"
                  :details res})))))

(defn ->valid-input! [h]
  "validate :data key for errors"
  (fn [{res :data :as req}]
    (println "->valid-input!")
    (let [errors (f/errors res)]
      (if (empty? errors)
        (h (assoc req :data res))
        (apply outcome 422
               "Resource Unprocessable Entity"
               (map
                 (fn [e] {:severity "fatal"
                          :details (str e)})
                 errors))))))

(defn ->check-deleted! [h]
  (fn [{{tp :type id :id} :params :as req}]
    (println "->check-deleted!")
    (if (db/-deleted? tp id)
      (outcome 410 "Resource was deleted"
               {:severity "fatal"
                :details (str "Resource " tp " with " id " was deleted")})
      (h req))))

(defn- check-latest-version [cl]
  (println "check-latest-version " cl)
  (let [[_ cl-] (cs/split cl (re-pattern (env/env :fhirplace-web-url)))]
    (let [[_ tp id vid] (cs/split cl- #"/")]
      (println "check-latest " tp " " id " " vid)
      (db/-latest? tp id vid))))

(defn ->latest-version! [h]
  (fn [{{tp :type id :id} :params :as req}]
    (println "->latest-version!")
    (if-let [cl (get-in req [:headers "content-location"])]
      (if (check-latest-version cl)
        (h req)
        (outcome 412 "Updating not last version of resource"
                 {:severity "fatal"
                  :details (str "Not last version")}))

      (outcome 401 "Provide 'Content-Location' header for update resource"
               {:severity "fatal"
                :details (str "No 'Content-Location' header")}))))

(def uuid-regexp
  #"[0-f]{8}-([0-f]{4}-){3}[0-f]{12}")

(defn =metadata [req]
  {:body (f/conformance)})

(defn =profile [{{tp :type} :params :as req}]
  {:body (f/profile-resource tp)})

(defn =search [{{rt :type :as param} :params}]
  {:body (db/-search rt (dissoc param :type))})

(defn =tags [req]
  {:body (db/-tags)})

(defn =resource-type-tags [{{rt :type} :params}]
  {:body (db/-tags rt)})

(defn =resource-tags [{{rt :type id :id} :params}]
  {:body (db/-tags rt id)})

(defn =resource-version-tags [{{rt :type id :id vid :vid} :params}]
  {:body (db/-tags rt id vid)})

(defn ->check-tags [h]
  (fn [{tags :tags :as req}]
    (if (seq tags)
      (h req)
      (outcome 422 "Tags"
               {:severity "fatal"
                :details (str "Expected not empty tags (i.e. Category header)")}))) )

;;TODO make as middle ware
(defn =affix-resource-tags [{{rt :type id :id} :params tags :tags}]
  (db/-affix-tags rt id tags)
  {:body (db/-tags rt id)})

(defn =affix-resource-version-tags [{{rt :type id :id vid :vid} :params tags :tags}]
  (db/-affix-tags rt id vid tags)
  {:body (db/-tags rt id vid)})

(defn =remove-resource-tags [{{rt :type id :id} :params}]
  (let [num (db/-remove-tags rt id)]
    {:body (str num " tags was removed")}))

(defn =remove-resource-version-tags [{{rt :type id :id vid :vid} :params}]
  (let [num (db/-remove-tags rt id vid)]
    {:body (str num " tags was removed")}))

(defn =history [{{rt :type id :id} :params}]
  {:body (db/-history rt id)})


(defn resource-resp [res]
  ( let [fhir-res (f/parse (:content res))
         tags (if (:category res)
                (json/read-str (:category res) :key-fn keyword)
                [])
         loc (url (.getResourceType fhir-res) (:logical_id res) (:version_id res))]
    (-> {:body fhir-res}
        (header "Location" loc)
        (header "Content-Location" loc)
        (header "Category" (fc/encode-tags tags))
        (header "Last-Modified" (:last_modified_date res)))))

(defn =create
  [{{rt :type} :params res :data tags :tags :as req}]
  {:pre [(not (nil? res))]}
  (println "=create " (keys req))
  (let [json (f/serialize :json res)
        jtags (json/write-str tags)
        item (db/-create (str (.getResourceType res)) json jtags)]
    (-> (resource-resp item)
        (status 201)
        (header "Category" (fc/encode-tags tags)))))

(defn =validate-create
  [{res :data tags :tags}]
  #_{:pre [(not (nil? res))]}
  {:status 200})

(defn =validate-update
  [{res :data}]
  #_{:pre [(not (nil? res))]}
  {:status 200})

(defn =update
  [{{rt :type id :id} :params res :data tags :tags}]
  {:pre [(not (nil? res))]}
  (let [json (f/serialize :json res)
        item (db/-update rt id json (json/write-str tags))]
    (-> (resource-resp item)
        (status 200))))

(defn =delete
  [{{rt :type id :id} :params body :body}]
  (-> (response (str (db/-delete rt id)))
      (status 204)))

;;TODO add checks
(defn =read [{{rt :type id :id} :params}]
  (let [res (db/-read rt id)]
    (-> (resource-resp res)
        (status 200))))

(defn =vread [{{rt :type id :id vid :vid} :params}]
  (let [res (db/-vread rt id vid)]
    (println res)
    (-> (resource-resp res)
        (status 200))))
