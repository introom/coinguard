(ns app.wallet.add-alert
  (:require
   ["react" :refer [createRef]]
   [reagent.core :as r]
   [cljs.core.async :as a]
   [cljs.reader :refer [read-string]]
   [com.rpl.specter :as s]
   [goog.string :as gstr]
   [app.logging :as log]
   [app.i18n :refer [tr]]
   [app.style :refer [tw]]
   [app.util.dev :as dev]
   [app.state :refer [app-state]]
   [app.util.http :as http]
   [app.ui.text :as text]
   [app.util.data :as d]
   [app.ui.widget :as wid]
   [app.ui.component :as comp]))

(defn- alert-address-view
  [addr]
  [comp/view {:style (tw "h-10 flex-row items-center bg-sky-200")}
   [comp/text {:style
               (tw "ml-3 text-xl font-semibold")}
    (gstr/format "%s:" (tr "alert-home/wallet-address"))]
   [comp/text {:style
               (tw "text-lg font-bold text-red-900 ml-3"
                   (d/js {:font-family text/font-family-monospace}))}
    (subs addr 2 10)]])

(defn- condition-item
  [navigation obj]
  (let [{:keys [item index _separators]} (d/clj obj)
        type (:type item)
        value (:value item)]
    [comp/pressable {:style
                     (fn [evt]
                       (tw "px-3 py-2 flex-row items-center"
                           (and (.-pressed evt) "bg-slate-200")))
                     :on-pressed #(.navigate navigation "edit-alert")}
     [comp/view {:style (tw "pl-4")}
      [comp/text {:style (tw "text-[18px] text-gray-700 font-bold")}
       (gstr/format "%s: %s" (tr "add-alert/input-cond-type-name") type)]
      [comp/text {:style [(tw "mt-1 text-[18px] text-[#881717] font-semibold")]}
       (gstr/format "%s: %s" (tr "add-alert/input-cond-value") value)]]]))

(defn- condition-list-separator
  []
  [comp/view {:style
              (tw "h-[px] bg-green-700")}])

(defn- save-alerts
  [wallet wallet'])

(defn- input-alert-name
  [wallet']
  [comp/view {:style (tw "pl-3 flex-row items-center bg-sky-300 h-10")}
   [comp/text {:style (tw "text-xl font-semibold")}
    (tr "add-alert/alert-name")]
   [comp/text-input {:on-change-text #(s/setval [s/ATOM :data :alerts s/LAST :name] % wallet')
                     :style (tw "text-xl leading-tight h-8 w-46 border rounded border-zinc-500 pl-2 ml-10")}]])

(defn- condition-list-view
  [^js navigation wallet wallet']
  [comp/view {:style (tw "grow-1")}
   [comp/view {:style (tw "flex-row justify-between items-center")}
    [wid/header-button :chevron-back-circle-outline #(.goBack navigation)]
    [wid/header-title (tr "add-alert/header-title")]
    [wid/header-button
     :save-outline #(save-alerts wallet wallet') {:size 28}]]
   [wid/header-rule]
   [alert-address-view (get-in @wallet' [:data :address])]
   [input-alert-name wallet']
   [comp/flat-list
    {:data (s/select-one [s/ATOM :data :alerts s/LAST :conditions] wallet')
     :render-item (partial condition-item navigation)
     :item-separator-component condition-list-separator
     :list-footer-component condition-list-separator}]])

(def ^:private field-text-style
  (tw "text-[18px] text-gray-800 font-semibold w-24"))

(def ^:private base-input-style
  (tw "mt-5 flex-row items-center justify-between w-[80%]"))

(defn- cond-type-picker
  [id state]
  (r/with-let [default-value "percent"
               types ["percent" "amount"]
               _ (swap! state assoc id default-value)]
    [comp/view {:style [base-input-style (tw "mt-20")]}
     [comp/text {:style field-text-style}
      (.toUpperCase (str (tr "add-alert/input-cond-type-name") ":"))]
     [comp/picker {:item-style (tw "w-50 h-10 font-semibold text-[18px] left-1")
                   :selected-value (id @state)
                   :on-value-change (fn [val _idx] (swap! state assoc id val))}
      (for [type types]
        [comp/picker-item {:key type :label (gstr/capitalize type) :value type}])]]))

(defn- input-section
  [name id ref state text-input-props]
  (r/with-let [is-active (r/atom false)]
    [comp/view {:style [base-input-style]}
     [comp/text {:style field-text-style}
      name]
     [comp/text-input (merge {:style
                              [(tw "text-xl leading-tight h-10 w-46 border-2 rounded border-zinc-700 pl-2")
                               (and @is-active (tw "border-blue-900"))]
                              :ref ref
                              ;; no need to show the previous value
                              ;; :default-value (id @state)
                              :on-change-text #(swap! state assoc id %)
                              :on-focus #(reset! is-active true)
                              :on-blur #(reset! is-active false)}
                             text-input-props)]]
    (finally
      (swap! state dissoc id))))

(defn- append-condition
  [{:keys [cond-value-ref state wallet']}]
  (let [cond-type (:cond-type-name @state)
        cond-value (:cond-value @state)]
    (s/transform [s/ATOM :data :alerts s/LAST :conditions]
                 #(conj (or % []) {:type cond-type :value cond-value})
                 wallet'))

  (doseq [ref [cond-value-ref]]
    (.. ref -current clear)))

(defn add-condition-modal
  [state wallet']
  (r/with-let [cond-value-ref (createRef)
               _ (swap! state assoc :loading? false)]
    [comp/modal {:is-visible (:cond-visible? @state)
                 :animation-type :slide
                 :backdrop-opacity 0.1
                 ;; :swipe-direction [:down]
                 ;; :on-swipe-complete #(swap! state assoc :cond-visible? false)
                 :style (tw "m-0 justify-end")}
     [comp/view {:style (tw "bg-sky-50 h-[50%] rounded-2xl p-3 items-center")}
      ;; dropdown
      [comp/pressable {:style (tw "items-center") :on-press #(swap! state assoc :cond-visible? false)}
       [comp/ion-icons {:name :chevron-down-sharp :color "black" :size 32}]]
      ;; inputs
      [cond-type-picker :cond-type-name state]
      [input-section
       (.toUpperCase (str (tr "add-alert/input-cond-value") ":")) :cond-value cond-value-ref state {}]
      ;; button
      [comp/pressable {:on-press (partial append-condition {:cond-value-ref cond-value-ref
                                                            :state state
                                                            :wallet' wallet'})
                       :style
                       (fn [evt]
                         (tw "mt-auto mb-10 bg-sky-500 rounded h-12 justify-center w-[70%]"
                             (and (.-pressed evt) "bg-sky-300")
                             (and (:loading? @state) "bg-sky-100")))}
       [comp/text {:style [(tw "text-[20px] text-center text-white font-medium")]}
        (tr "app/add")]]]]))

(defn add-alert-screen
  [{:keys [navigation route]}]
  {:pre [(dev/register-component :ui/add-alert)]}
  (r/with-let [state (r/atom {:cond-visible? false})]
    (let [params (-> (.-params route) read-string)
          wallet-path (:wallet-path params)
          wallet (r/cursor app-state wallet-path)
          wallet' (r/atom (update-in @wallet [:data :alerts] (fnil conj []) {:name "New alert" :conditions []}))]
      [comp/safe-area-view {:edges ["top"] :style (tw "grow-1")}
       [condition-list-view navigation wallet wallet']
       [comp/view {:style (tw "items-center")}
        [comp/pressable {:on-press #(swap! state assoc :cond-visible? true)}
         [comp/ion-icons {:name "add-circle" :color "#21a44dd3" :size 56}]]
        [add-condition-modal state wallet']]])))