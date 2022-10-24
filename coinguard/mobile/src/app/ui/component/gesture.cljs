(ns app.ui.component.gesture
  (:require
   [reagent.core :as r]
   [app.ui.component.util :refer [props-children]]
   ["react-native-gesture-handler/Swipeable$default" :as Swipeable]))

(def ^:private swipeable-class (r/adapt-react-class Swipeable))

(defn- adapt-swipeable-props
  [{:keys [render-right-actions render-left-actions]}]
  (merge (when render-right-actions
           {:render-right-actions (fn [obj] (r/as-element [render-right-actions obj]))})
         (when render-left-actions
           {:render-left-actions (fn [obj] (r/as-element [render-right-actions obj]))})))

;; see https://docs.swmansion.com/react-native-gesture-handler/docs/api/components/swipeable
(defn swipeable
  [& xs]
  (let [[props children] (props-children xs)]
    (into [swipeable-class (merge props (adapt-swipeable-props props))]
          children)))
