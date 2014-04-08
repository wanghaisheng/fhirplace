(ns fhirplace.search-test
  (use [clojure.core.match :only (match)]
       [clojure.pprint :only (pprint)])
  (require [clojure.string :as string]
           [honeysql.core :as sql]
           [honeysql.helpers :refer :all]))
;; Type            R-value                  Modifiers   JOIN                                    Where
;[ number       [true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;               [    num]                 []           join table_name                         table_name.field = num
;               [:<  num]                 []           join table_name                         table_name.field < num
;               [:>  num]                 []           join table_name                         table_name.field > num
;               [:<= num]                 []           join talbe_name                         table_naem.field <= num
;               [:>= num]                 []           join talbe_name                         table_name.field >= num
;
;  date         [true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;               [    datetime]            []           join table_name                         table_name.field = datetime with handling precision
;               [:<  datetime]            []           join table_name                         table_name.field < datetime with handling precision
;               [:>  datetime]            []           join table_name                         table_name.field > datetime with handling precision
;               [:<= datetime]            []           join table_name                         table_name.field <= datetime with handling precision
;               [:>= datetime]            []           join table_name                         table_name.field >= datetime with handling precision
;
;  string       [true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;               [string]                  []           join table_name                         table_name.field ilike '%string%' (should be opensearch ranking)
;               [string]                  [:exact]     join table_name                         table_name.field = 'string'
;
;  token        [true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;               [string]                  [:text]      join table_name                         table_name.(coding=display | codable_concept=text | identifier=label) ilike '%string%'
;               [code]                    []           join table_name                         table_name.code = code or table_name.value = code
;               [ns :| code]              []           join table_name                         table_name.namespace (= ns or is null) and table_name.code = code
;
;  reference    [true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;               [id]                      [:type]      join table_name                         table_name.reference = id and check type
;               [id]                      []           join table_name                         table_name.reference = id
;               [url]                     []           join table_name                         table_name.reference = extract id from url
;
;  composite    [op :, op]                []           join table_name                         table_name.field in (op, op ...)
;               [prop-1 :$ op :,            
;                prop2 :$ op]             []           need recursive call for every part of value
;
;  quantity     [true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;               [comp num :| ns :| code]  []           join table_name                         table_name.field comp need to convert quantities
; ]

(def number-query "subject:Patient.name:exact=Peter")
(def quantity-query "value:exact=5.4|http://unitsofmeasure.org|mg")

(defn tokenize [query]
  (let [[l-value r-value] (string/split query #"=")
        [field & modifier] (string/split l-value #":")]
    [field (string/join ":" modifier) r-value]))

(defn field-type [resource-type field]
  :string)

(defn gen-matching-seq [tokenized-query resource-type]
  (let [[field modifier r-value] tokenized-query
        the-field-type (field-type resource-type field)]
    [resource-type field the-field-type modifier r-value]))

(tokenize number-query)
(tokenize quantity-query)

; ("Patient" "value" :number :exact "5.4|http://unitsofmeasure.org|mg") )

(defn ++ [& keywords]
  (keyword (apply str (map name keywords))))


(defn left-join* [alias & parts]
  (let [parts (map string/lower-case parts)]
    (left-join [(++ :fhir. (string/join "_" parts)) alias]
               [:= (++ alias :._logical_id) :_root._logical_id])))

(defn join* [alias & parts]
  (let [parts (map string/lower-case parts)]
    (join [(++ :fhir. (string/join "_" parts)) alias]
          [:= (++ alias :._version_id) :_root._version_id])))

(left-join* :t "Patient" "name")

(++ :k :l :w :c "_sdasda")

(defn gen-search-sql [[res-type field field-type modifier r-value :as tks]]
  (match [field field-type modifier r-value]

    [field _        "missing" r-value] (-> (left-join* :t res-type field)
                                           (where [(if (= "false" r-value) :not= :=)
                                                   :t._id :null]))
    [field :number  _         r-value] (-> (join* :t res-type field)
                                           (where [:= (++ :t. field) r-value]))
    [field :string  _         r-value] (-> (join* :t res-type "name")
                                           (where [:= (str "%" r-value "%") (++ :%any.t. field)]))
         
    ))

(pprint (map (fn [x] (-> x
                 tokenize
                 (gen-matching-seq "Patient")
                 gen-search-sql))
     ["value:missing=true"
      "family=john"]))

(-> "family=john"
    tokenize
    (gen-matching-seq "Patient")
    gen-search-sql
    (select :a1.family :_root._logical_id)
    (from [:fhir.view_patient_full :_root])
    sql/format)

;[ number       [true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;               [    num]                 []           join table_name                         table_name.field = num
;               [string]                  []           join table_name                         table_name.field ilike '%string%' (should be opensearch ranking)

