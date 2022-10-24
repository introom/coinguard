(ns app.wallet.alert-home
  (:require
   [reagent.core :as r]
   [cljs.core.async :as a]
   [cljs.reader :refer [read-string]]
   [goog.string :as gstr]
   [app.logging :as log]
   [app.i18n :refer [tr]]
   [app.style :refer [tw]]
   [app.util.dev :as dev]
   [app.state :refer [app-state]]
   [app.util.http :as http]
   [app.ui.text :as text]
   [app.util.data :as d]
   [app.util.resource :refer [js-require]]
   [app.ui.widget :as wid]
   [app.ui.component :as comp]))

(def ^:private animal-icon-list
  [(js-require "assets/alert-img/0.png")
   (js-require "assets/alert-img/1.png")
   (js-require "assets/alert-img/2.png")
   (js-require "assets/alert-img/3.png")
   (js-require "assets/alert-img/4.png")
   (js-require "assets/alert-img/5.png")
   (js-require "assets/alert-img/6.png")
   (js-require "assets/alert-img/7.png")
   (js-require "assets/alert-img/8.png")
   (js-require "assets/alert-img/9.png")
   (js-require "assets/alert-img/10.png")
   (js-require "assets/alert-img/11.png")
   (js-require "assets/alert-img/12.png")
   (js-require "assets/alert-img/13.png")
   (js-require "assets/alert-img/14.png")])

(def ^:private animal-icon-list-count (count animal-icon-list))

(defn- delete-alert-item
  [alerts index]
  (swap! alerts (fn [v] (into (subvec v 0 index)
                              (subvec v (inc index))))))

(defn- alert-address-view
  [addr]
  [comp/view {:style (tw "h-10 flex-row items-center bg-sky-200")}
   [comp/text {:style
               (tw "text-xl font-semibold")}
    (gstr/format "%s:" (tr "alert-home/wallet-address"))]
   [comp/text {:style
               (tw "ml-3 text-lg font-bold text-red-900"
                   (d/js {:font-family text/font-family-monospace}))}
    (subs addr 2 10)]])

(defn- delete-alert-view
  [alerts index]
  [comp/pressable {:on-press (partial delete-alert-item alerts index)
                   :style
                   (fn [evt]
                     (tw "bg-red-500 justify-center pl-4"
                         (and (.-pressed evt) "bg-red-900")))}
   [comp/text {:style (tw "text-white font-semibold text-base")}
    (tr "app/delete")]])

(defn- alert-item
  [alerts navigation obj]
  (let [{:keys [item index _separators]} (d/clj obj)
        name (get-in item [:name])
        conditions (get-in item [:conditions])]
    (log/info "Render item" :item item)
    [comp/swipeable {:render-right-actions (partial delete-alert-view alerts index)
                     :overshoot-friction 10}
     [comp/pressable {:style
                      (fn [evt]
                        (tw "px-3 py-2 flex-row items-center"
                            (and (.-pressed evt) "bg-slate-200")))
                      :on-pressed #(.navigate navigation "edit-alert")}
      [comp/image {:source (animal-icon-list (mod index animal-icon-list-count))
                   :style [(tw "w-12 my-[-24px]")
                           (d/js {:resize-mode "contain"})]}]
      [comp/view {:style (tw "pl-4")}
       [comp/text {:style (tw "text-[18px] text-gray-700 font-bold")}
        name]
       [comp/text {:style [(tw "mt-1 text-[18px] text-[#881717] font-semibold")]}
        (gstr/format "%s: %s" (tr "alert-home/condition-count") (count conditions))]]]]))

(defn- alert-list-separator
  []
  [comp/view {:style
              (tw "h-[px] bg-green-700")}])

(defn- alert-list-view
  [^js navigation wallet-path]
  (r/with-let [wallet (r/cursor app-state wallet-path)]
    [comp/view {:style (tw "grow-1")}
     [comp/view {:style (tw "flex-row justify-between items-center")}
      [wid/header-button :chevron-back-circle-outline #(.goBack navigation)]
      [wid/header-title (tr "alert-home/header-title")]
      [wid/header-button :add-circle-outline #(.navigate navigation
                                                         "add-alert"
                                                         (pr-str {:wallet-path wallet-path}))]]
     [wid/header-rule]
     [alert-address-view (get-in @wallet [:data :address])]
     [comp/flat-list
      {:data (get-in @wallet [:data :alerts])
       :render-item (partial alert-item (r/cursor wallet [:data :alerts]) navigation)
       :item-separator-component alert-list-separator
       :list-footer-component alert-list-separator}]]))

(defn alert-home-screen
  [{:keys [navigation route]}]
  {:pre [(dev/register-component :ui/alert-home)]}
  (let [params (-> (.-params route) read-string)
        wallet-path (:wallet-path params)]
    [comp/safe-area-view {:edges ["top"] :style (tw "grow-1")}
     [alert-list-view navigation wallet-path]]))
