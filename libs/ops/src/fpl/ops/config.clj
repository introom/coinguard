(ns fpl.ops.config
  (:require
   [clojure.edn :as edn]
   [fpl.ops.sops :as sops]
   [fpl.clj.cli :refer [parse-opts]]
   [fpl.clj.process :refer [environ get-env]]))

(defn make-context
  "Context is composed of command line arguments, sops encrypted 
   file configuration and environment variables."
  []
  (let [opts (parse-opts *command-line-args*)
        config (some-> (get-env :app-ops-config-file nil)
                       sops/decrypt
                       edn/read-string)]
    (merge environ config opts)))

(defn resource-name
  "Adds `env` name to resource."
  [name]
  (format "%s-%s" name (get-env :app-env)))

(comment
  (resource-name "ingress-nginx"))