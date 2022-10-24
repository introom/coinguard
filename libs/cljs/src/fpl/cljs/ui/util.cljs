(ns fpl.cljs.ui.util
  (:require
   [reagent.core :as r]))

(defn props-children
  [[x & xs :as coll]]
  (if (or (nil? x) (map? x))
    [x xs]
    [nil coll]))

(defn wrap-with-props
  [class-tag default-props]
  (let [class-tag (cond-> class-tag (not (keyword? class-tag)) r/adapt-react-class)]
    (fn [& xs]
      (let [[props children] (props-children xs)]
        (into [class-tag (merge default-props props)] children)))))

(defn functional-component
  [fun]
  (fn [& xs]
    (into [:f> fun] xs)))