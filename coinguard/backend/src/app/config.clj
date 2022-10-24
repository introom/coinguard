(ns app.config
  (:refer-clojure :exclude [get])
  (:require
   [clojure.core :as core]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [com.rpl.specter :as s]
   [environ.core :refer [env]]))

(def ^:private defaults
  {:c/env :local
   :c/db-url "" #_"https://localhost:3000"
   :c/db-username ""
   :c/db-password ""
   :c/http-host "127.0.0.1"
   :c/http-port 8080
   :c/eth-node-url "http://localhost:18543"
   :c/twilio-account-sid ""
   :c/twilio-auth-token ""
   :c/twilio-from-account ""})

(defn- read-file
  [file]
  (when file
    (->> (slurp file)
         (edn/read-string)
        ;; add the `:c/` namespace
         (s/transform s/MAP-KEYS #(keyword "c" (name %))))))

(defn- read-env
  [prefix]
  (let [prefix (str prefix "-")
        len (count prefix)]
    (reduce-kv
     (fn [acc k v]
       (cond-> acc
         (str/starts-with? (name k) prefix)
         ;;  add the ":c/" namespace
         (assoc (keyword "c" (subs (name k) len)) v)))
     {}
     env)))

(def ^:private version (or (some-> (io/resource "VERSION")
                                   slurp
                                   str/trim)
                           "develop"))

(defn- read-config
  []
  ;; sorted so that it looks good in the console
  (let [config (atom (sorted-map :version version))
        mf (read-file (:app-config-file env))
        me (read-env "app")]
    (doseq [[k v] defaults]
      (let [vf (k mf)
            ve (k me)
            val (cond
                  (keyword? v) (or (keyword ve) vf v)
                  (string? v) (or ve vf v)
                  (int? v) (or (some-> ve parse-long) vf v)
                  (fn? v) (v @config)
                  :else (or ve vf v))]
        (swap! config assoc k val)))
    @config))

(def ^:dynamic config (read-config))

(defn get
  [key]
  (core/get config key))

(comment
  (read-config)
  (get :c/http-host))