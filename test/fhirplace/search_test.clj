(ns fhirplace.search-test
  (require [clojure.string :as string]
           [honeysql.core :as h]
           [clojure.core.match :as m]
           [clojure.pprint :as p]
           [honeysql.helpers :as hh]))
;; Type            R-value                  Modifiers   JOIN                               Where
;select v.* from fhir.view_patient_full v
;+[ number       [true | false]            [:missing]   left join table_name        table_name._id is null | table_name._id is not null
;+               [    num]                 []           join table_name             table_name.field = num
;+               [:<  num]                 []           join table_name             table_name.field < num
;+               [:>  num]                 []           join table_name             table_name.field > num
;+               [:<= num]                 []           join talbe_name             table_naem.field <= num
;+               [:>= num]                 []           join talbe_name             table_name.field >= num
; 
;+  date         [true | false]            [:missing]   left join table_name        table_name._id is null | table_name._id is not null
;+               [    datetime]            []           join table_name             table_name.field = datetime with handling precision
;                [:<  datetime]            []           join table_name             table_name.field < datetime with handling precision
;                [:>  datetime]            []           join table_name             table_name.field > datetime with handling precision
;                [:<= datetime]            []           join table_name             table_name.field <= datetime with handling precision
;                [:>= datetime]            []           join table_name             table_name.field >= datetime with handling precision
; 
;+  string       [true | false]            [:missing]   left join table_name        table_name._id is null | table_name._id is not null
;                [string]                  []           join table_name             table_name.field ilike '%string%' (should be opensearch ranking)
;+               [string]                  [:exact]     join table_name             table_name.field = 'string'
; 
;+  token        [true | false]            [:missing]   join fhir.patient_gender(full path) j1 on j1._version_id = v._version_id where v._state = 'current' and j1._id is (not) null;
;                [string]                  [:text]      join fhir.patient_gender(full path)  table_name.(coding=display | codable_concept=text | identifier=label) ilike '%string%'
; resource-type, search param full path, fhir path type (Coding, CodableConcept, Identifier)
;                [code]                    []           join fhir.patient_gender(full path)  table_name.code = code or table_name.value = code
;                [ns :| code]              []           join table_name             table_name.namespace (= ns or is null) and table_name.code = code
; 
;+  reference    [true | false]            [:missing]   left join table_name        table_name._id is null | table_name._id is not null
;                [id]                      [:type]      join table_name             table_name.reference = id and check type
;                [id]                      []           join table_name             table_name.reference = id
;                [url]                     []           join table_name             table_name.reference = extract id from url
; 
;   composite    [op :, op]                []           join table_name             table_name.field in (op, op ...)
;                [prop-1 :$ op :,            
;                 prop2 :$ op]             []           
; 
;+  quantity     [true | false]            [:missing]   left join table_name        table_name._id is null | table_name._id is not null
;                [comp num :| ns :| code]  []           join table_name             table_name.field comp need to convert quantities
; ]


(defn ++ [& keywords]
  (keyword (apply str (map name keywords))))


(defn left-join* [alias parts]
  (let [parts (map string/lower-case parts)]
    (hh/left-join [(++ :fhir. (string/join "_" parts)) alias]
               [:= (++ alias :._logical_id) :_root._logical_id])))

(defn join* [alias parts]
  (let [parts (map string/lower-case parts)]
    (hh/join [(++ :fhir. (string/join "_" parts)) alias]
             [:= (++ alias :._version_id) :_root._version_id])))


(defn split-r-value [r-value]
  (rest
    (re-find #"([^\|]+(?=\|))?(\|)?([^\|]+)"
             r-value)))

(defn sql-path [search-path]
  (string/split
    (string/lower-case search-path)
    #"\."))

(defn token-to-sql
  [resource-type search-path fhir-type modifier r-value]
  (let [r-value (vec (split-r-value r-value))
        search-path (sql-path search-path)
        token-complex-value-search-query (fn [search-path value uri value-attr with-system?]
                                     (-> (join* :t1 search-path)
                                         (hh/where (if with-system?
                                                     [:and
                                                      [:= (++ :t1. value-attr) value]
                                                      [:= :t1.system uri]]
                                                     [:= (++ :t1 value-attr) value]))))
        token-complex-text-search-query (fn [search-path value text-attr]
                                          (-> (join* :t1 search-path)
                                              (hh/where [:= (++ :t1. text-attr) value])))]
    (m/match
      [fhir-type modifier r-value]

      ["Coding"                 nil              [uri sep value]]   (token-complex-value-search-query
                                                                      search-path value uri :code sep) 

      ["Coding"                 :text            [_ _ value]]       (token-complex-text-search-query
                                                                      search-path value :display)
      [(:or "Coding"
            "Identifier"
            "CodeableConcept")  :missing         [_ _ value]]       (-> (left-join* :t1 search-path)
                                                                        (hh/where [(if (= value "true") := :not=) 
                                                                                   :t1._id nil]))

      ["Identifier"             nil              [uri sep value]]   (token-complex-value-search-query
                                                                      search-path value uri :value sep)

      ["Identifier"             :text            [_ _ value]]       (token-complex-text-search-query
                                                                      search-path value :label)

      ["CodeableConcept"        nil              [uri sep value]]   (token-complex-value-search-query
                                                                      (concat search-path) value uri :value sep)

      ["CodeableConcept"        :text            [_ _ value]]       (token-complex-value-search-query
                                                                      search-path value :text)

      [_                        (:or nil
                                     :text)      [_ _ value]]       (-> (join* :t1 (butlast search-path))
                                                                        (hh/where [:= 
                                                                                   (h/raw
                                                                                     (str "t1." (last search-path) "::varchar"))
                                                                                   value]))

      [_                        :missing         [_ _ value]]       (-> (join* [:t1 (butlast search-path)]
                                                                              [:not= (keyword (last search-path)) nil])
                                                                        (hh/where [(if (= "true" value) := :not=)
                                                                                   :t1._id nil])))))


(-> (token-to-sql
      "Patient" "Patient.gender" "Coding" :text "M")
    (hh/select :*)
    (hh/from :table )
    (h/format))
      
(defn uncertain
  [value]
  (let  [number  (Double/parseDouble (str value))
         precision  (count  (second  (clojure.string/split  (str value) #"\.")))
         delta  (* 5  (/ 1  (Math/pow 10  (+ precision 1))))]
    [number (- number delta) (+ number delta)]))

; number:
; (let [value low high (uncertain parameter)])
; =       join  (res path) j where j. (last path) >= low and j. (last path) < high
; <       join  (res path) j where j. (last path) < value
; <=      join  (res path) j where j. (last path) <= value
; >       join  (res path) j where j. (last path) > value
; >=      join  (res path) j where j. (last path) >= value
; missing join  (res path) j on j. (last path) is not null where j._id is not? null

; need to pass which parameters where processed to form valid outcome
