(ns app.i18n
  (:require
   [app.util.resource :refer [js-require]]
   ;; see https://www.npmjs.com/package/i18n-js
   ["i18n-js" :as i18n]
   ["react-native-localize" :as localize]
   [goog.object :as gobj]))

(defn require-language
  [lang]
  (let [lang-key (keyword lang)]
    (case lang-key
      :en (js-require "i18n/en.json")
      :zh (js-require "i18n/zh.json"))))

(defn load-language
  [lang]
  (when-not (gobj/get (.-translations i18n) lang)
    (try
      (gobj/set (.-translations i18n)
                lang
                (require-language lang))
      (catch :default _))))

(defn set-language
  [lang]
  (load-language lang)
  (set! (.-locale i18n) lang))

(defn tr
  ([key]
   (tr key {}))
  ([key opts]
   (let [label (name key)]
     (.t i18n label (clj->js opts)))))

(def default-language
  (gobj/get (first (.getLocales localize)) "languageCode"))


(defn- set-language-event-handler
  [^js event]
  (set-language (.-language event)))

(defn setup
  []
  ;; this is actually the default value.
  (set! (.-defaultSeparator i18n) ".")
  (load-language "en")
  (set! (.-default_locale i18n) "en")
  (set! (.-fallbacks i18n) true)
  (set-language default-language)

   ;; per upstream doc, localize will only add the SAME callback once
   (.addEventListener localize "change" set-language-event-handler))

(comment
  (.-fallbacks i18n)
  (load-language "zh")
  (.-translations i18n)
  (.getLocales localize)
  (.t i18n "sign-in")
  (tr :sign-in))