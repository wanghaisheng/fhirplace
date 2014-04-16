(ns fhirplace.views.resources
  (:require
    [fhirplace.views.common :as c]
    [hiccup.core :as h]
    [clojure.string :as s]
    [hiccup.page :as p]))

(defn view [xs]
  (c/layout
    [:h1 "Resources"]
    [:table.table
     [:tr
      [:th.nowrap "Id"]
      (for [[k v] (dissoc (first xs) :data)]
        [:th (str k)])]
     (for [x xs]
       [:tr
        [:td.nowrap
         [:a {:href (str "/" (:resource_type x) "/" (:_logical_id x))} (:_logical_id x)]]
        (for [[k v] (dissoc x :data)]
          [:td (str v)])])]))

(defn show [res]
  (c/layout
    [:h1 "Resource "
     [:small
      [:a {:href "?_format=application/xml"} "XML"]
      " "
      [:a {:href "?_format=application/json"} "JSON"]]]
    (c/pretty-res res)))
