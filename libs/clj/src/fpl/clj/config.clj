;; atm, for a clean management, we choose to NOT support incorporating command line arguments.
(ns fpl.clj.config
  (:refer-clojure :rename {get core-get})
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [com.rpl.specter :as sp]
   [fpl.clj.process :refer [environ]]
   [fpl.ops.sops :as sops]))

(def example-defaults
  "Example defaults for illustration purpose.
   
   `array-map` is used to maintain order (dependency) among environment variables."
  (array-map
   :c/env :local
   :c/foo "foo"
   :c/bar 42
   :c/baz (fn [config] (inc (:c/bar config)))))

(defn- read-file
  "File content shall be:
   ```
   {:c/foo \"foo\"
   ,,,}
   ```"
  [file encryption]
  (when file
    (let [content (case encryption
                    :sops (sops/decrypt file)
                    nil (->> (slurp file)
                             (edn/read-string)))]
      ;; ensure the namespace prefix ":c"
      (sp/transform sp/MAP-KEYS #(keyword "c" (name %)) content))))

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
     environ)))

(def ^:private version (or (some-> (io/resource "VERSION")
                                   slurp
                                   str/trim)
                           "develop"))

(defn- read-config
  "See `fpl.clj.config/example-defaults` for the format."
  ([defaults]
   (read-config defaults nil))
  ([defaults {:keys [env-prefix config-file encryption]}]
   ;; sorted so that the config map looks good in the console
   (let [config (atom (sorted-map :c/version version))
         me (read-env (:env-prefix env-prefix))
         mf (read-file config-file encryption)]
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
     @config)))

(def ^:dynamic *config* nil)

(defn setup
  ([defaults]
   (setup defaults {}))
  ([defaults {:keys [env-prefix config-file encryption]
              :or {env-prefix "app" config-file (:app-config-file environ)}}]
   (alter-var-root #'*config* (constantly (read-config defaults {:env-prefix env-prefix
                                                                 :config-file config-file
                                                                 :encryption encryption})))))

(defn get
  [x]
  (core-get *config* x))

(comment
  (read-config example-defaults)
  (read-config example-defaults {:env-prefix "myapp" :config-file :myapp-config-file}))
