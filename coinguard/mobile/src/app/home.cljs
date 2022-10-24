(ns app.home
  (:require
   [reagent.core :as r]
   ["@react-navigation/bottom-tabs" :refer (createBottomTabNavigator)]
   [app.wallet :refer [wallet-stack]]
   [app.util.data :as d]
   [app.util.dev :as dev]
   [app.i18n :refer [tr]]
   [app.ui.component :as comp]
   [app.profile :refer [profile-tab]]))

(defn- tab-bar-icon
  [screen]
  (let [tab-icons
        {:wallet "md-wallet-outline"
         :profile "md-person-outline"}]
    (fn [props]
      (let [{:keys [_focused color size]} (d/clj props)]
        (r/as-element
         [comp/ion-icons {:name (screen tab-icons) :color color :size size}])))))

;; see https://reactnavigation.org/docs/bottom-tab-navigator
(def ^:private screen-options
  {:header-shown false
   :tab-bar-active-tint-color "blueviolet"
   :tab-bar-inactive-tint-color "dimgray"
   :tab-bar-label-style {:position :relative :top 0}})

(defn home-tab
  []
  {:pre [(dev/register-component :ui/home-tab)]}
  (let [tab (createBottomTabNavigator)]
    [:> (.-Navigator tab) {:screen-options screen-options}
     [:> (.-Screen tab) {:name "wallet"
                         :options {:title (tr "home/wallet-tab-name")
                                   :tab-bar-icon (tab-bar-icon :wallet)}
                         :component (r/reactify-component wallet-stack)}]
     [:> (.-Screen tab) {:name "profile"
                         :options {:title (tr "home/profile-tab-name")
                                   :tab-bar-icon (tab-bar-icon :profile)}
                         :component (r/reactify-component profile-tab)}]]))
