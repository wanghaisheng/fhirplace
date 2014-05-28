(ns fhirplace.search-test
  (require [clojure.string :as string]
           [clojure.java.jdbc :as jdbc]
           [honeysql.core :as h]
           ; [clojure.core.match :as m]
           [fhirplace.system :as sys]
           [fhirplace.repositories.resource :as repo]
           [clojure.test.check :as tc]
           [clojure.test.check.properties :as prop]
           [clojure.test.check.generators :as gen]
           [clojure.test.check.clojure-test :as ct]
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


; (defn ++ [& keywords]
;   (keyword (apply str (map name keywords))))


; (defn left-join* [alias parts]
;   (let [parts (map string/lower-case parts)]
;     (hh/left-join [(++ :fhir. (string/join "_" parts)) alias]
;                [:= (++ alias :._logical_id) :_root._logical_id])))

; (defn join* [alias parts & others]
;   (let [parts (map string/lower-case parts)]
;     (hh/join [(++ :fhir. (string/join "_" parts)) alias]
;              [:= (++ alias :._version_id) :_root._version_id]
;              others)))


; (defn split-r-value [r-value]
;   (rest
;     (re-find #"([^\|]+(?=\|))?(\|)?([^\|]+)"
;              r-value)))

; (defn sql-path [search-path]
;   (string/split
;     (string/lower-case search-path)
;     #"\."))
; (butlast (sql-path "Patient.active"))
; (defn fhir-type-for-search-path [search-path]
;   (condp = search-path
;     "Patient.active" "boolean"
;     "Patient.animal.breed" "CodeableConcept"
;     "Patient.gender" "CodeableConcept"
;     "Patient.animal.species" "CodeableConcept"
;     "Patient.identifier" "Identifier"
;     "Patient.communication" "CodeableConcept"))


; (defn token-to-sql
;   [resource-type search-path-str modifier r-value]
;   (let [r-value (vec (split-r-value r-value))
;         search-path (sql-path search-path-str)
;         fhir-type (fhir-type-for-search-path search-path-str)
;         complex-value-search-query (fn [search-path value uri value-attr with-system?]
;                                      (-> (join* :t1 search-path)
;                                          (hh/where (if with-system?
;                                                      [:and
;                                                       [:= (++ :t1. value-attr) value]
;                                                       [:= :t1.system uri]]
;                                                      [:= (++ :t1. value-attr) value]))))
;         complex-text-search-query (fn [search-path value text-attr]
;                                     (-> (join* :t1 search-path)
;                                         (hh/where [:= (++ :t1. text-attr) value])))]

;     (m/match
;      [fhir-type modifier r-value]

;      ["Coding"                 nil              [uri sep value]]   (complex-value-search-query
;                                                                     search-path value uri :code sep)

;      ["Coding"                 :text            [_ _ value]]       (complex-text-search-query
;                                                                     search-path value :display)
;      [(:or "Coding"
;            "Identifier"
;            "CodeableConcept")  :missing         [_ _ value]]       (-> (left-join* :t1 search-path)
;                                                                        (hh/where [(if (= value "true") := :not=)
;                                                                                   :t1._id nil]))

;      ["Identifier"             nil              [uri sep value]]   (complex-value-search-query
;                                                                     search-path value uri :value sep)

;      ["Identifier"             :text            [_ _ value]]       (complex-text-search-query
;                                                                     search-path value :label)

;      ["CodeableConcept"        nil              [uri sep value]]   (complex-value-search-query
;                                                                     (concat search-path) value uri :value sep)

;      ["CodeableConcept"        :text            [_ _ value]]       (complex-text-search-query
;                                                                     search-path value :text)

;      [_                        (:or nil
;                                     :text)      [_ _ value]]       (-> (join* :t1 (butlast search-path))
;                                                                        (hh/where [:=
;                                                                                   (h/raw
;                                                                                    (str "t1." (last search-path) "::varchar"))
;                                                                                   value]))

;      [_                        :missing         [_ _ value]]       (-> (join* :t1 (butlast search-path)
;                                                                               [:not= (keyword (last search-path)) nil])
;                                                                        (hh/where [(if (= "true" value) := :not=)
;                                                                                   :t1._id nil])))))
; (def test-db (:db (sys/create :test)))

; (defn any-of [args]
;   (gen/one-of (map gen/return args)))

; (defn execute! [resource-type search-path modifier r-value]
;   (jdbc/query test-db
;               (-> (token-to-sql resource-type search-path modifier r-value)
;                   (hh/select :*)
;                   (hh/from [:fhir.view_patient_full :_root])
;                   h/format)))

; (tc/quick-check
;  1
;  (prop/for-all
;   [resource-type (gen/return "Patient")
;    search-path (any-of ["Patient.active" "Patient.animal.breed"
;                         "Patient.animal.species" "Patient.gender"
;                         "Patient.identifier" "Patient.communication"])
;    modifier (any-of [:text nil :missing])
;    r-value (gen/return "true")]

;   (execute! resource-type search-path modifier r-value)))

; (token-to-sql "Patient" "Patient.active" :missing "true")



; (defn uncertain
;   [value]
;   (let  [number  (Double/parseDouble (str value))
;          precision  (count  (second  (clojure.string/split  (str value) #"\.")))
;          delta  (* 5  (/ 1  (Math/pow 10  (+ precision 1))))]
;     [number (- number delta) (+ number delta)]))

; number:
; (let [value low high (uncertain parameter)])
; =       join (res path) j where j.(last path) >= low and j. (last path) < high
; <       join (res path) j where j.(last path) < value
; <=      join (res path) j where j.(last path) <= value
; >       join (res path) j where j.(last path) > value
; >=      join (res path) j where j.(last path) >= value
; missing left join (res path) j on j.(last path) is not null where j._id is not? null

; string:
;         join (res path) j where j.(last path) = value
; exact   join (res path) j where j.(last path) ilike value
; missing left join (res path) j on j.(last path) is not null where j._id is not? null

; reference:
; get value from id or uri
;         join path j where j.reference = value
; type    join path j where j.reference = value
; missing left join path j where j._id is not? null

; quantity:
; we assume that all quantities are intervals
; we search for interval intersection
; [comparator][number]|[namespace]|[code]
; comparator?= join (++ str :_quantity) j where j. interval comparison
; ~            join (++ str :_quantity) j where j. interval comparison +- 10%
; missing left join (++ str :_quantity) j where j._id is not? null

; date:
; yyyy-mm-ddThh:nn:ss(TZ)

; first need to find intersection of (date, dateTime, instant, Period, Schedule) and (multitype attribute)
; operator     jeft join (res path) j-s jeft join (++ path _period) j-p jeft join (++ path _schedule_repeat) j-r where j-s.(last path) operatior value or j-p intersection with perion or j-r intersection with scheduler
; missing left join (res path) j on j.(last path) is not null where j._id is not? null

; need to pass which parameters where processed to form valid outcome

; for multitype observation.value[x] there is exception on join and field part value_string or separate table observation_value_period etc

; head of query for selecting:
; select v.json, v_logical_id from fhir.view_[resource-type]
; chaining to connect conditions and selecting:
; join fhir.resource r1 on r._logical_id = v._logical_id and r._state = 'current'
; join fhir.path1 p1 on p1._version_id = r._version_id
; join fhir.resource r2 on r._logical_id = p1.reference and r._state = 'current'
; join fhir.path2 p2 on p2._version_id = r2._version_id
; last in chain for conditions:
; join fhir.resource _root on _root._logical_id = p2.reference
; join .... conditions
; where conditions


; for cases like FR,NL in where expressions we use form (code = 'FR') or (code = 'NL') ||| (code in ('FR', 'NL'))
; for composite type we flatten expression to separate search parameters like:
; from   | state-on-date=new$2013-05-04,active$2013-05-05
; we get | new=2013-05-04 and active=2013-05-05
; and process them as regular search parameters
