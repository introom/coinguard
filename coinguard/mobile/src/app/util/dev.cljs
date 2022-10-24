(ns app.util.dev
  (:require
   [reagent.core :as r]
   [app.state :refer [app-state]]))

(defn register-component
  [key]
  (swap! app-state assoc key (r/current-component))
  true)

(defn refresh-component
  [key]
  (when-some [comp (key @app-state)]
    (r/force-update comp true)))