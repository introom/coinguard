(ns app.welcome.login
  (:require
   [reagent.core :as r]
   [app.logging :as log]
   [app.style :refer [tw]]
   [app.state :as state :refer [app-state]]
   [app.ui.component :as comp]
   [app.i18n :refer [tr]]
   [app.util.async :as aa]
   [cljs.core.async :as a]
   [app.config :as config]
   [app.util.http :as http]))

(def cred-cursor (r/cursor app-state [:app/credential]))

(defn- input-section
  [name text-input-props state]
  (r/with-let [active? (r/atom false)]
    [comp/view {:style
                (tw "mt-2")}
     [comp/text {:style
                 (tw "text-lg text-gray-800 font-bold")}
      name]
     [comp/text-input (merge {:style (tw "text-xl leading-tight h-10 border-2 rounded border-zinc-700"
                                         (and @active? "border-blue-900"))
                              :on-change-text #(reset! state %)
                              :on-focus #(reset! active? true)
                              :on-blur #(reset! active? false)}
                             text-input-props)]]))

(defn- sign-up
  [& {:keys [email password mobile cred loading? canceled? error]}]
  (reset! loading? true)
  (reset! error nil)
  (a/go
    (try
      (let [sign-up-url (config/get :c/api-create-account-url)
            data {:email email :password password :mobile mobile}
            resp (aa/<p! (http/post sign-up-url {:data data}))
            {:keys [token account-id]} (:data resp)]

        (when-not @canceled?
          (reset! cred {:account-id account-id
                        :token token})
          ;; write back to disk
          (state/write-disk :app/credential)))
      (catch :default e
        (reset! error (tr "login/sign-up-failed"))
        (log/error "Sign up failed." :err e))
      (finally
        (reset! loading? false)
        (log/info "Sign up finished.")))))

(defn sign-up-screen
  []
  (r/with-let [loading? (r/atom false)
               error (r/atom nil)
               canceled? (atom false)
               [email password mobile] (repeatedly #(atom nil))]
    [comp/view {:style (tw "bg-sky-200 h-full items-center")}
     [comp/view {:style (tw "w-7/12 max-w-[270px] mt-[40%]")}
      [comp/text {:style (tw "font-bold text-3xl")}
       (tr "login/headline-sign-up")]

      [comp/view {:style (tw "mt-10")}
       [input-section (tr "email") {:keyboard-type "email-address"} email]

       [input-section (tr "password") {:secure-text-entry true} password]

       [input-section (tr "mobile") {:keyboard-type "numeric"} mobile]]

      [comp/view {:style (tw "mt-15 h-6")}
       (and @error
            [comp/text {:style (tw "text-red-600 font-bold text-sm")}
             @error])]

      [comp/pressable {:on-press #(sign-up :email @email
                                           :password @password
                                           :mobile @mobile
                                           :cred cred-cursor
                                           :loading? loading?
                                           :canceled? canceled?
                                           :error error)
                       :style
                       (fn [evt]
                         (tw "mt-5 bg-sky-500 rounded h-12 justify-center items-center flex-row"
                             (and (.-pressed evt) "bg-sky-600")
                             (and @loading? "bg-gray-400")))}
       (and @loading?
            [comp/activity-indicator {:color "yellow"
                                      :style
                                      [(tw "absolute left-[10%] top-1/2")
                                       {:transform [{:scale 1.5}
                                                    {:translateY -6}]}]}])
       [comp/text {:style
                   [(tw "text-2xl text-center text-slate-800 font-medium")]}
        (tr "login/sign-up-btn")]]]]
    (finally
      (reset! canceled? true))))

(defn- sign-in
  [& {:keys [email password cred loading? canceled? error]}]
  (reset! loading? true)
  (reset! error nil)
  (a/go
    (try
      (let [sign-in-url (config/get :c/api-create-session-url)
            data {:email email :password password}
            resp (aa/<p! (http/post sign-in-url {:data data}))
            {:keys [token account-id]} (:data resp)]
        (when-not @canceled?
          (reset! cred {:account-id account-id
                        :token token})
          ;; write back to disk
          (state/write-disk :app/credential)))
      (catch :default e
        (reset! error (tr "login/sign-in-failed"))
        (log/error "Sign in failed." :err e))
      (finally
        (reset! loading? false)
        (log/info "Sign in finished.")))))


(defn sign-in-screen
  []
  (r/with-let [loading? (r/atom false)
               error (r/atom nil)
               canceled? (atom false)
               [email password] (repeatedly #(atom nil))]
    [comp/view {:style (tw "bg-sky-200 h-full items-center")}
     [comp/view {:style (tw "w-7/12 max-w-[270px] mt-[40%]")}
      [comp/text {:style (tw "font-bold text-3xl")}
       (tr "login/headline-sign-in")]

      [comp/view {:style (tw "mt-10")}
       [input-section (tr "email") {:keyboard-type "email-address"} email]

       [input-section (tr "password") {:secure-text-entry true} password]]

      [comp/view {:style (tw "mt-15 h-6")}
       (and @error
            [comp/text {:style (tw "text-red-600 font-bold text-sm")}
             @error])]

      [comp/pressable {:on-press #(sign-in :email @email
                                           :password @password
                                           :cred cred-cursor
                                           :loading? loading?
                                           :canceled? canceled?
                                           :error error)
                       :style
                       (fn [evt]
                         (tw "mt-5 bg-sky-500 rounded h-12 justify-center items-center flex-row"
                             (and (.-pressed evt) "bg-sky-600")
                             (and @loading? "bg-gray-400")))}
       (and @loading?
            [comp/activity-indicator {:color "yellow"
                                      :style
                                      [(tw "absolute left-[10%] top-1/2")
                                       {:transform [{:scale 1.5}
                                                    {:translateY -6}]}]}])
       [comp/text {:style
                   [(tw "text-2xl text-center text-slate-800 font-medium")]}
        (tr "login/sign-in-btn")]]]]
    (finally
      (reset! canceled? true))))

(comment
  (reset! cred-cursor {:app-token :dummy}))