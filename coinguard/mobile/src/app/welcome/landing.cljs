(ns app.welcome.landing
  (:require
   [app.logging :as log]
   [app.util.resource :refer [js-require]]
   [app.ui.component :as comp]
   [app.i18n :refer [tr]]
   [app.style :refer [tw]]))

(defonce landing-bg (js-require "assets/landing-bg.jpg"))

(defn landing-screen
  [{:keys [navigation]}]
  (let [base-text-style
        (tw "text-xl text-center font-medium mx-10 text-cyan-300")]
    [comp/image-background {:source landing-bg
                            :resize-mode :cover
                            :style (tw "grow-1")}
     [comp/view {:style (tw "flex-1 bg-[#000000a0] text-white")}
      [comp/view {:style (tw "grow-6 justify-end")}
       [comp/text {:style (tw "text-4xl text-stone-300 font-semibold text-center")}
        (tr "landing/slogan")]]
      [comp/view {:style (tw "grow-1 justify-start mt-3")}
       [comp/text {:style (tw "text-base text-stone-400 text-center")}
        (tr "landing/sub-slogan")]]
      [comp/view {:style (tw "grow-5 flex-row justify-center pt-60")}
       [comp/text {:style [base-text-style]
                   :on-press #(.navigate navigation "page/sign-up")}
        (tr "sign-up")]
       [comp/text {:style [base-text-style]
                   :on-press #(.navigate navigation "page/sign-in")}
        (tr "sign-in")]]]]))