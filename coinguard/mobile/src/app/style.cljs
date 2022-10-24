(ns app.style
  (:require
   [app.util.resource :refer [js-require]]
   ["twrnc" :as twrnc]))

;; the path is relative to the resources/ directory
(def tw-inst (twrnc/create (js-require "../tailwind.config.js")))

(defn tw
  [& args]
  (apply (.-style tw-inst) args))

(defn setup
  []
  ;; see https://github.com/jaredh159/tailwind-react-native-classnames#enabling-device-context-prefixes
  (.useDeviceContext twrnc tw-inst))

(comment
  (tw-inst.color "blue-100")
  (tw "flex-1" #js {:resizeMode "repeast"
                    :width 3})
  (tw "dark:bg-black")
  (tw "ios:top-[12px] bg-[#07B5D3]")
  (tw "ios:pt-4 android:pt-2")
  (tw "flex-col lg:flex-row")
  (tw "bg-white dark:bg-black")
  (tw "p-4"))
