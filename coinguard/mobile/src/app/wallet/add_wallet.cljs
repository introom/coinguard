(ns app.wallet.add-wallet
  (:require
   ["react" :refer [createRef]]
   [app.config :as config]
   [app.i18n :refer [tr]]
   [app.logging :as log]
   [app.style :refer [tw]]
   [app.state :refer [app-state]]
   [app.ui.component :as comp]
   [app.ui.widget :as wid]
   [cljs.core.async :as a]
   [app.util.async :as aa]
   [app.util.http :as http]
   [reagent.core :as r]))

(def ^:private field-text-style
  (tw "text-base text-gray-800 font-bold"))

(defn- input-section
  [name id ref state text-input-props]
  (r/with-let [is-active (r/atom false)]
    [comp/view {:style (tw "mt-2")}
     [comp/text {:style field-text-style}
      name]
     [comp/text-input (merge {:style (tw "text-xl leading-tight h-10 border-2 rounded border-zinc-700 pl-2"
                                         (and @is-active "border-blue-900"))
                              :ref ref
                              :default-value (id @state)
                              :on-change-text #(swap! state assoc id %)
                              :on-focus #(reset! is-active true)
                              :on-blur #(reset! is-active false)}
                             text-input-props)]]))

(defn- coin-type-picker
  [id state]
  (let [default-value "ethereum"]
    @(delay (swap! state assoc id default-value))
    [comp/view {:style (tw "mt-2")}
     [comp/text {:style field-text-style}
      (tr "add-wallet/input-coin-type")]
     [comp/picker {:item-style (tw "mx-[-10px] h-12 font-semibold text-[18px]")
                   :selected-value "ethereum"
                   :on-value-change (fn [val _idx] (swap! state assoc id val))}
      [comp/picker-item {:label "Ethereum" :value "ethereum"}]]]))

(defn- handle-submit
  [state {:keys [name-ref address-ref]}]
  (a/go
    (try
      (let [create-wallet-url (config/get :c/api-create-wallet-url)
            data {:account-id (get-in @app-state [:app/credential :account-id])
                  :data {:name (:name state)
                         :coin (:coin state)
                         :address (:address state)}}
            _resp (aa/<p! (http/post create-wallet-url {:data data}))
            _ (prn _resp)])
      (catch :default e
        (log/error "Creation failed." :err e))))

  (doseq [ref [name-ref address-ref]]
    (.. ref -current clear)))

(defn add-wallet-screen
  [{:keys [navigation]}]
  (r/with-let [loading? (r/atom false)
               state (r/atom {:name nil
                              :address nil
                              :coin nil})]
    (let [[name-ref address-ref]  (repeatedly #(createRef))]
      [comp/safe-area-view {:edges ["top"] :style (tw "grow-1")}
       [comp/keyboard-avoiding-view {:style (tw "grow-1")}
        [comp/view {:style (tw "grow-1")}
         [comp/view {:style (tw "flex-row justify-between items-center")}
          [wid/header-title (tr "add-wallet/header-title")]
          [wid/header-button :chevron-back-circle-outline #(.goBack navigation)]]
         [wid/header-rule]
         [comp/linear-gradient {:colors ["#4c669f00", "#798bb16c", "#313a4d00"]
                                :style (tw "grow-1 pt-6 px-12")}
          [input-section
           (tr "add-wallet/input-wallet-name") :name name-ref state {}]
          [coin-type-picker :coin state]
          [input-section
           (tr "add-wallet/input-address") :address address-ref state {}]

          [comp/pressable {:on-press (partial handle-submit state {:name-ref name-ref
                                                                   :address-ref address-ref})
                           :style
                           (fn [evt]
                             ;; https://stackoverflow.com/a/31006659/855160
                             ;; see the auto-margin trick.
                             (tw "mt-auto mb-50 bg-[#6b5c7e] rounded h-12 justify-center w-full"
                                 (and (.-pressed evt) "bg-sky-600")
                                 (and @loading? "bg-gray-400")))}
           [comp/text {:style [(tw "text-2xl text-center text-white font-medium")]}
            (tr "add-wallet/submit")]]]]]])))




