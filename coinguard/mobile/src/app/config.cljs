(ns app.config
  (:refer-clojure :exclude [get])
  (:require-macros [app.config :refer [get-config-file-name]])
  (:require
   [clojure.core :as core]
   [goog.string :as gstr]
   [app.util.resource :refer-macros [js-require]]))

(defn- join-url
  [base-url-kw sub-url]
  (fn [config]
    (gstr/format "%s/%s" (core/get config base-url-kw) sub-url)))

(def ^:private defaults
  ;; use array-map to maintain order. O(n) time complexity for lookup.
  ;; https://github.com/clj-commons/ordered is faster.
  (array-map
   :c/api-base-url "https://localhost:3000"
   :c/api-create-account-url (join-url :c/api-base-url "accounts")
   :c/api-create-session-url (join-url :c/api-base-url "session")
   :c/api-create-wallet-url (join-url :c/api-base-url "wallets")))

(defn- read-config
  []
  (let [m (->> (js-require (get-config-file-name))
               js->clj
               ;; prepend the namespace :c/
               (into {} (map (fn [[k v]] [(keyword "c" k) v]))))
        config (atom (sorted-map))]
    (doseq [[k v] defaults]
      (let [v2 (k m)
            val (cond
                  (string? v) (or v2 v)
                  (int? v) (or (some-> v2 js/parseInt) v)
                  (fn? v) (v @config)
                  :else (or v2 v))]
        (swap! config assoc k val)))
    @config))

(def ^:dynamic config (read-config))

(defn get
  [key]
  (core/get config key))

(comment
  (get :c/api-create-session-url))