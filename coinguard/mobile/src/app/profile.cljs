(ns app.profile
  (:require
   [app.style :refer [tw]]
   [app.ui.component :as comp]))

(defn profile-tab
  []
  [comp/view {:style (tw "grow-1 justify-center items-center")}
   [comp/text {:style (tw "text-3xl text-green-900 font-semibold")}
    "This page is under review."]])