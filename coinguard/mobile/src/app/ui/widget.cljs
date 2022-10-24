(ns app.ui.widget
  (:require
   [app.style :refer [tw]]
   [app.ui.component :as comp]))

(defn header-title
  [title]
  [comp/text {:style
              (tw "ml-3 font-bold text-2xl text-slate-900")}
   title])

(defn header-button
  [icon-kw on-press-fn style]
  [comp/pressable {:on-press on-press-fn
                   :style
                   (fn [_evt] (tw "mx-3"))}
   [comp/ion-icons (merge {:name icon-kw
                           :color "#2b1c46ff"
                           :size 32}
                          style)]])

(defn header-rule
  []
  [comp/view {:style
              (tw "border-b mt-1 border-b-blue-900")}])