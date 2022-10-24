(ns fpl.ops.sops
  (:require
   [fpl.clj.process :refer [sh]]))

(defn decrypt
  "The environment variable such as `SOPS_AGE_KEY` shall be set beforehand."
  [path]
  (-> (sh (format "sops -d %s" path) {:out :string})
      :out))
