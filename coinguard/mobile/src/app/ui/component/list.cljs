(ns app.ui.component.list
  (:require
   [reagent.core :as r]
   [app.util.data :as d]
   ["react-native" :as rn]))

(def ^:private flat-list-class (r/adapt-react-class rn/FlatList))

;; NB. note that the naming is mostly of PascalCase not camelCase.
(defn- adapt-flat-list-props
  [{:keys [render-item item-separator-component list-empty-component
           list-header-component list-footer-component]}]
  (merge (when render-item
           {:renderItem #(r/as-element [render-item %])})
         (when item-separator-component
           {:ItemSeparatorComponent #(r/as-element [item-separator-component %])})
         (when list-empty-component
           {:ListEmptyComponent #(r/as-element [list-empty-component])})
         (when list-header-component
           {:ListHeaderComponent #(r/as-element [list-header-component])})
         (when list-footer-component
           {:ListFooterComponent #(r/as-element [list-footer-component])})))

(defn flat-list
  [{:keys [data] :as props}]
  [flat-list-class
   (merge props
          (adapt-flat-list-props props)
          {:data (to-array data)})])