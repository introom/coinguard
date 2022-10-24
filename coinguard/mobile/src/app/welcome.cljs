(ns app.welcome
  (:require
   [app.logging :as log]
   [reagent.core :as r]
   ["@react-navigation/stack" :refer [createStackNavigator]]
   [app.welcome.landing :refer [landing-screen]]
   [app.welcome.login :refer [sign-up-screen sign-in-screen]]))

(defn welcome-stack
  []
  (log/info "welcome screen")
  (let [stk (createStackNavigator)]
    [:> stk.Navigator {:screen-options {:header-shown false}}
     [:> stk.Group
      [:> stk.Screen {:name "page/landing"
                      :component (r/reactify-component landing-screen)}]]
     [:> stk.Group {:screen-options {:presentation :modal}}
      [:> stk.Screen {:name "page/sign-up"
                      :component (r/reactify-component sign-up-screen)}]
      [:> stk.Screen {:name "page/sign-in"
                      :component (r/reactify-component sign-in-screen)}]]]))