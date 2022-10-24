(ns app.wallet.wallet-home
  (:require
   [reagent.core :as r]
   [cljs.core.async :as a]
   [app.logging :as log]
   [app.i18n :refer [tr]]
   [app.style :refer [tw]]
   [app.util.dev :as dev]
   [app.state :refer [app-state]]
   [app.util.http :as http]
   [app.util.data :as d]
   [app.ui.widget :as wid]
   [app.ui.text :as text]
   [app.ui.component :as comp]))

;; :start -> :processing -> :done
(defonce initial-loading (r/atom :start))

(defn- fetch-wallets
  []
  (a/go
    (a/<! (a/timeout 300))))

(defn- loading-view
  []
  (log/info "Loading screen")
  (when (= @initial-loading :start)
    (reset! initial-loading :processing)
    (a/go
      (a/<! (fetch-wallets))
      (reset! initial-loading :done)))
  [comp/view {:style
              [(tw "grow-1 justify-center items-center")]}
   [comp/activity-indicator {:color "#8814146a" :size "large"}]
   [comp/text {:style (tw "mt-6")}
    (tr "wallet-home/loading-content")]])

(defn- delete-wallet-item
  [wallets index]
  (swap! wallets (fn [v] (into (subvec v 0 index)
                               (subvec v (inc index))))))

(defn- delete-wallet-view
  [wallets index]
  [comp/pressable {:on-press (partial delete-wallet-item wallets index)
                   :style
                   (fn [evt]
                     (tw "bg-red-500 justify-center pl-4"
                         (and (.-pressed evt) "bg-red-900")))}
   [comp/text {:style (tw "text-white font-semibold text-base")}
    (tr "app/delete")]])

(defn- wallet-item
  [wallets navigation obj]
  (let [{:keys [item index _separators]} (d/clj obj)
        name (get-in item [:data :name])
        address (get-in item [:data :address])
        address-len (count address)
        abbr-address (str (subs address 0 15)
                          "..."
                          (subs address (- address-len 6) address-len))]
    (log/debug "Render item" :item item)
    [comp/swipeable {:render-right-actions (partial delete-wallet-view wallets index)
                     :overshoot-friction 10}
     [comp/pressable {:style
                      (fn [evt]
                        (tw "px-3 py-2 flex-row items-center"
                            (and (.-pressed evt) "bg-slate-200")))
                      :on-press #(.navigate navigation
                                            "alert-home"
                                            (pr-str {:wallet-path [:wallet/list index]}))}
      [comp/material-icons {:name "ethereum" :color "#d92164ff" :size 40}]
      [comp/view {:style (tw "pl-4")}
       [comp/text {:style (tw "text-[18px] text-gray-700 font-bold")}
        name]
       [comp/text {:style [(tw "mt-1 text-[20px] text-[#881717] font-light")
                           (d/js {:font-family text/font-family-monospace})]}
        abbr-address]]]]))

(defn- wallet-list-separator
  []
  [comp/view {:style
              (tw "h-[px] bg-green-700")}])

(defn- wallet-list-view
  [navigation]
  (r/with-let [wallets (r/cursor app-state [:wallet/list])]
    [comp/view {:style (tw "grow-1")}
     [comp/view {:style (tw "flex-row justify-between items-center")}
      [wid/header-title (tr "wallet-home/header-title")]
      [wid/header-button :add-circle-outline #(.navigate navigation "add-wallet")]]
     [wid/header-rule]
     [comp/flat-list
      {:data @wallets
       :render-item (partial wallet-item wallets navigation)
       :item-separator-component wallet-list-separator
       :list-footer-component wallet-list-separator}]]))

(defn wallet-home-screen
  [{:keys [navigation _route]}]
  {:pre [(dev/register-component :ui/wallet-home)]}
  ;; see https://github.com/th3rdwave/react-native-safe-area-context/issues/107#issuecomment-652616230.  
  ;; basically safe-area does not know the existence of the bottom tab bar, so here
  ;; we explicitly disable the bottom padding.
  [comp/safe-area-view {:edges ["top"] :style (tw "grow-1")}
   (if (not= @initial-loading :done)
     [loading-view]
     [wallet-list-view navigation])])

(comment
  (ns-name :ab/cd)
  (require 'cljs.reader)
  (-> (pr-str {:wallet-path :wallet/list})
      cljs.reader/read-string)
  (d/js {:wallet-path :wallet/list}))

