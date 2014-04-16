(ns fhirplace.views.metadata
  (:require
    [fhirplace.views.common :as c]
    [hiccup.core :as h]
    [clojure.string :as s]
    [hiccup.page :as p]))



(defn view [conf]
  (let [info (dissoc conf :rest)
        api (get-in conf [:rest 0])
        resources (sort-by :type (:resources api))
        api (dissoc api :resources)]
    (h/html
      (c/layout
        [:h2 "Conformance "
         [:small
          [:a {:href "/metadata?_format=application/xml"} "XML"]
          " "
          [:a {:href "/metadata?_format=application/json"} "JSON"]]]
        (c/pretty-res info)

        [:h2 "Rest API"]
        (c/pretty-res api)
        [:h2 "Resources"]
        [:table.table
         [:thead
          [:tr
           [:th "Type"]
           [:th "History"]
           [:th "Update/Create"]
           [:th "Search"]
           [:th "Operation"]]]
         [:tbody
          (for [res resources]
            [:tr
             [:td [:a {:href (str "/" (:type res) "/_search") } (str (:type res))]]
             [:td (str (:readHistory res))]
             [:td (str (:updateCreate res))]
             [:td (str (:searchInclude res))]
             [:td (str (s/join ", " (map :type (:operation res))))]]
            )]]
        ))))
