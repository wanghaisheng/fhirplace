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

(defn search-using [op res-type field r-value]
  (-> (join* :t res-type field)
      (hh/where [op (++ :t. field) r-value])))

;#_(defn gen-search-sql [[res-type field field-type modifier r-value :as tks]]
;  (m/match [field field-type modifier (vec r-value)]
;
;    [field _          :missing [nil value & _]] (-> (left-join* :t res-type field)
;                                                    (hh/where [(if (= "false" value) :not= :=)
;                                                               :t._id :null]))
;    [field :number    _        [nil value & _]] (search-using := res-type field value)
;
;    [field :number    _        [op value & _]]  (search-using op res-type field value)
;
;    [field :datetime  _        [nil value & _]] (search-using := res-type field value)
;
;    [field :string    :exact   [nil value & _]] (search-using :=  res-type field value)
;
;    [field :string    _        [nil value & _]] (-> (join* :t res-type "name")
;                                                   (hh/where [:= (str "%" value "%") (++ :%any.t. field)]))
;    ))
;
;
;
;(p/pprint (map (fn [x] (-> x
;                 tokenize
;                 (gen-matching-seq "Patient")
;                 gen-search-sql))
;     ["value:missing=true"
;      "family=john"]))
;
;(string/split "family=<=2012" #"=" 2)
;
;(-> "family=<=2012"
;    tokenize
;    (gen-matching-seq "Patient")
;    gen-search-sql
;    (hh/select :a1.family :_root._logical_id)
;    (hh/from [:fhir.view_patient_full :_root])
;    h/format)

;[ number       [true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;               [    num]                 []           join table_name                         table_name.field = num
;               [string]                  []           join table_name                         table_name.field ilike '%string%' (should be opensearch ranking)
; (count (second (clojure.string/split (str x) #"\."))))

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
        search-path (sql-path search-path)]

    (m/match
      [fhir-type modifier r-value]

      ["Coding"                 nil              [uri sep value]]   (-> (join* :t1 search-path)
                                                                        (hh/where (if sep
                                                                                    [:and
                                                                                     [:= :t1.code value]
                                                                                     [:= :t1.system uri]]
                                                                                    [:= :t1.code value])))

      ["Coding"                 :text            [_ _ value]]       (-> (join* :t1 search-path)
                                                                        (hh/where [:= :t1.display value]))

      [(:or "Coding"
            "Identifier"
            "CodeableConcept")  :missing         [_ _ value]]       (-> (left-join* :t1 search-path)
                                                                        (hh/where [(if (= value "true") := :not=) 
                                                                                   :t1._id nil]))

      ["Identifier"             nil              [uri sep value]]   (-> (join* :t1 search-path)
                                                                        (hh/where (if sep
                                                                                    [:and
                                                                                     [:= :t1.value value]
                                                                                     [:= :t1.system uri]]
                                                                                    [:= :t1.value value])))

      ["Identifier"             :text            [_ _ value]]       (-> (join* :t1 search-path)
                                                                        (hh/where [:= :t1.label value]))

      ["CodeableConcept"        nil              [uri sep value]]   (-> (join* :t1 (concat search-path :coding))
                                                                        (hh/where (if sep
                                                                                    [:and
                                                                                     [:= :t1.code value]
                                                                                     [:= :t1.system uri]]
                                                                                    [:= :t1.code value])))

      ["CodeableConcept"        :text            [_ _ value]]       (-> (join* :t1 search-path)
                                                                        (hh/where [:= :t1.text value]))

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

    ;;(hh/join [(++ :fhir. (string/join "_" parts)) alias]
    ;;         [:= (++ alias :._version_id) :_root._version_id])))

