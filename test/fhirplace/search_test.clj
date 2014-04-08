; {resource-type
; {search-parameter type}
; }
; 
; ; Type            R-value                  Modifiers   JOIN                                    Where
; { number       {[true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;                 [    num]                 []           join table_name                         table_name.field = num
;                 [:<  num]                 []           join table_name                         table_name.field < num
;                 [:>  num]                 []           join table_name                         table_name.field > num
;                 [:<= num]                 []           join talbe_name                         table_naem.field <= num
;                 [:>= num]                 []}          join talbe_name                         table_name.field >= num
; 
;   date         {[true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;                 [    datetime]            []           join table_name                         table_name.field = datetime with handling precision
;                 [:<  datetime]            []           join table_name                         table_name.field < datetime with handling precision
;                 [:>  datetime]            []           join table_name                         table_name.field > datetime with handling precision
;                 [:<= datetime]            []           join table_name                         table_name.field <= datetime with handling precision
;                 [:>= datetime]            []}          join table_name                         table_name.field >= datetime with handling precision
; 
;   string       {[true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;                 [string]                  []           join table_name                         table_name.field ilike '%string%' (should be opensearch ranking)
;                 [string]                  [:exact]}    join table_name                         table_name.field = 'string'
; 
;   token        {[true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;                 [string]                  [:text]      join table_name                         table_name.(coding=display | codable_concept=text | identifier=label) ilike '%string%'
;                 [code]                    []           join table_name                         table_name.code = code or table_name.value = code
;                 [ns :| code]              []}          join table_name                         table_name.namespace (= ns or is null) and table_name.code = code
; 
;   reference    {[true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;                 [id]                      [:type]      join table_name                         table_name.reference = id and check type
;                 [id]                      []           join table_name                         table_name.reference = id
;                 [url]                     []}          join table_name                         table_name.reference = extract id from url
; 
;   composite    {[op :, op]                []           join table_name                         table_name.field in (op, op ...)
;                 [prop-1 :$ op :,            
;                  prop2 :$ op]             []}          need recursive call for every part of value
; 
;   quantity     {[true | false]            [:missing]   left join table_name                    table_name._id is null | table_name._id is not null
;                 [comp num :| ns :| code]  []}          join table_name                         table_name.field comp need to convert quantities
;  }
; 
; (defn build-where params
;   "")
; 
; [type modif r-value]
; 
; [:qunatity nil "<5.4 | http.. | mg"] =>
;   "where value < 5.4 and system = http and unit = mg"
; [:number :missing "true"] =>
;   "where age is null"
; [:number :missing "false"] =>
;   "where age is null"
; [:composite nil "name$23,value$23"]
;    self name $ 23 "or" ...
