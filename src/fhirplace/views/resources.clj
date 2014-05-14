(ns fhirplace.views.resources
  (:require
    [fhirplace.views.common :as c]
    [hiccup.core :as h]
    [clojure.string :as s]
    [hiccup.page :as p]
    [hiccup.form :as f]))

(defn view [xs]
  (c/layout
   [:h1 "Resources"]
   (f/form-to [:post "/alert.json"] (f/text-area "resource" "{\"resourceType \": \"Alert \",\"text \": {\"status \": \"generated \",\"div \": \"<div>Large Dog warning</div> \"},\"category \": {\"coding \": [{\"system \": \"local \",\"code \": \"admin \",\"display \": \"Admin \"}],\"text \": \"admin \"},\"status \": \"active \",\"subject \": {\"reference \": \"Patient/example \",\"display \": \"Peter Patient \"},\"author \": {\"reference \": \"Practitioner/example \",\"display \": \"Nancy Nurse \"},\"note \": \"patient has a big dog at his home. Always always wear a suit of armor or take other active counter-measures \"}")
              (f/submit-button "Create Alert"))
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
