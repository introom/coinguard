(ns app.platform
  (:require ["react-native" :as rn]))

(def platform
  (.-Platform rn))

(def os (.-OS ^js platform))

(def android? (= os "android"))
(def ios? (= os "ios"))

(def version
  (when platform
    (.-Version ^js platform)))
