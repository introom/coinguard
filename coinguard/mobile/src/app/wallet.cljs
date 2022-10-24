(ns app.wallet
  (:require
   ["@react-navigation/stack" :refer [createStackNavigator]]
   [reagent.core :as r]
   [app.wallet.wallet-home :refer [wallet-home-screen]]
   [app.wallet.add-wallet :refer [add-wallet-screen]]
   [app.wallet.alert-home :refer [alert-home-screen]]
   [app.wallet.add-alert :refer [add-alert-screen]]))

(defn wallet-stack
  []
  (let [stk (createStackNavigator)]
    [:> stk.Navigator {:screen-options {:header-shown false}}
     [:> stk.Screen {:name "wallet-home"
                     :component (r/reactify-component wallet-home-screen)}]
     [:> stk.Screen {:name "add-wallet"
                     :component (r/reactify-component add-wallet-screen)}]
     [:> stk.Screen {:name "alert-home"
                     :component (r/reactify-component alert-home-screen)}]
     [:> stk.Screen {:name "add-alert"
                     :component (r/reactify-component add-alert-screen)}]]))