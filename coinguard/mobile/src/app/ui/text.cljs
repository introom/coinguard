(ns app.ui.text
  (:require
   ["react-native" :as rn :refer [Platform]]))

;; see ios font:
;; http://iosfonts.com/

(def font-family-monospace
  (if (= (.-OS Platform) "ios")
      "Menlo-Regular"
      "monospace"))