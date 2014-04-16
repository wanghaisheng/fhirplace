(ns fhirplace.views.common
  (:require
    [garden.core :refer [css]]
    [hiccup.core :as h]
    [hiccup.page :as p]))

(defn stylesheet [href]
  [:link {:href href :rel "stylesheet"}])

(defn style []
  (css
    [:body {:padding-top "20px"}]
    [:div.page-title {:border-bottom "1px solid #aaa"}]
    [:.nowrap {:white-space  "nowrap"}]))

(defn layout [& content]
  (p/html5
    [:head
     (stylesheet "http://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css")
     [:style (style)]]
    [:body
     [:div.container
      [:div.page-title
       [:a {:href "/"}
         [:h1 "FHIRPlace REST service"]]]
      content]]))

(defn pretty-res
  [obj]
  (cond
    (map? obj) [:ul.list
                (for [[k v] obj]
                  [:li
                   [:b (name k)] " " (pretty-res v)])]
    (vector? obj) [:ul.list
                   (for [v obj]
                     [:li (pretty-res v)])]

    :else (str obj)))
