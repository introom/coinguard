(ns app.ui.component
  (:require
   [reagent.core :as r]
   [app.ui.component.util :refer [props-children]]
   [app.ui.component.list :as list]
   [app.ui.component.gesture :as gesture]
   [app.platform :as platform]
   ["react" :as react]
   ["@react-native-picker/picker" :refer [Picker]]
   ["react-native-linear-gradient$default" :as LinearGradient]
   ["react-native-safe-area-context" :refer [SafeAreaView]]
   ["react-native-vector-icons/Ionicons$default" :as ion-icons-class]
   ["react-native-vector-icons/MaterialCommunityIcons$default" :as material-icons-class]
   ["react-native-modal$default" :as Modal]
   ["react-native" :as rn]))

;; no-op for now
(def provider (r/adapt-react-class react/Fragment))

(def view (r/adapt-react-class rn/View))
(def safe-area-view (r/adapt-react-class SafeAreaView))
(def text (r/adapt-react-class rn/Text))
(def image (r/adapt-react-class rn/Image))
(def image-background (r/adapt-react-class rn/ImageBackground))
;; do not use rn/Button, it even cannot specify background color.
(def pressable (r/adapt-react-class rn/Pressable))
(def activity-indicator (r/adapt-react-class rn/ActivityIndicator))
(def flat-list list/flat-list)

(defn text-input
  [& xs]
  (let [default-props
        {:auto-capitalize "none"
         :spell-check false
         :auto-correct false}
        [props children] (props-children xs)]
    (into [:> rn/TextInput (merge default-props props)] children)))

;; one can style on this component
;; https://github.com/oblador/react-native-vector-icons#styling
(def ion-icons (r/adapt-react-class ion-icons-class))
(def material-icons (r/adapt-react-class material-icons-class))

(def animated-text (r/adapt-react-class rn/Animated.Text))

(def swipeable gesture/swipeable)

(defn keyboard-avoiding-view
  [& xs]
  (let [[props children] (props-children xs)]
    (into [:> rn/KeyboardAvoidingView
           (merge (when platform/ios? {:behavior :padding})
                  props)]
          children)))

(def linear-gradient (r/adapt-react-class LinearGradient))

(def picker (r/adapt-react-class Picker))
(def picker-item (r/adapt-react-class (.-Item Picker)))

;; see https://github.com/react-native-modal/react-native-modal#available-props
(def modal (r/adapt-react-class Modal))