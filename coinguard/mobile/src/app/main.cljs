(ns app.main
  (:require
   [reagent.core :as r]
   ["react-native" :as rn :refer [AppRegistry]]
   ["react-native-safe-area-context" :refer [SafeAreaProvider]]
   ["@react-navigation/native" :refer [NavigationContainer]]
   ["@react-navigation/stack" :refer [createStackNavigator]]
   [cljs.core.async :as a]
   [app.i18n :as i18n]
   [app.util.dev :as dev]
   [app.state :as state :refer [app-state]]
   [app.logging :as log]
   [app.style :as style]
   [app.config]
   [app.ui.component :as comp]
   [app.welcome :refer [welcome-stack]]
   [app.home :refer [home-tab]]
   ;; see https://shadow-cljs.github.io/docs/UsersGuide.html#_using_npm_packages
   ["react-native-splash-screen$default" :as splash-screen]))

(defn- app
  []
  {:pre [(dev/register-component :ui/app)]}
  (log/info "app screen")
  ;; it uses a hook
  (style/setup)
  (let [stk (createStackNavigator)
        cred @(r/track #(:app/credential @app-state))]
    [comp/provider
     [:> SafeAreaProvider
      [:> NavigationContainer {:on-ready #(.hide splash-screen)}
       [:> stk.Navigator {:screen-options {:header-shown false}}
        (if-not (seq cred)
          [:> stk.Screen {:name "welcome"
                          :component (r/reactify-component welcome-stack)}]
          [:> stk.Screen {:name "home"
                          :component (r/reactify-component home-tab)}])]]]]))

(def app-name "coinguard")

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn -reload-app {:dev/after-load true}
  []
  (dev/refresh-component :ui/app)
  (dev/refresh-component :ui/home-tab)
  (dev/refresh-component :ui/wallet-home)
  (dev/refresh-component :ui/alert-home)
  (dev/refresh-component :ui/add-alert))

(defn- setup
  [props]
  (a/go
    ;; setup subsystems
    (i18n/setup)
    (a/<! (state/setup))
    ;; wrap in :>f to make a functional component
    (.registerComponent ^js AppRegistry app-name #(r/reactify-component (constantly [:f> app])))
    (.runApplication AppRegistry app-name props)))

(defn ^:export -main []
  (log/setup {:level :debug})
  (log/info "Coinguard started.")
  ;; see https://stackoverflow.com/a/62621360/855160
  ;; we use registerRunnable to delay runApplication until we have all subsystems finished loading.
  (.registerRunnable AppRegistry app-name setup))

;; see also https://github.com/thheller/reagent-react-native/blob/master/src/main/test/app.cljs#L34
(comment
  (.hide splash-screen)
  (.show splash-screen)
  (r/as-element [:strong "world! "])
  (log/info "Something is wrong"))
