(ns app.ui.component.util)

(defn props-children
  [[x & xs :as coll]]
  (if (or (nil? x) (map? x))
    [x xs]
    [nil coll]))
