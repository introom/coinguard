(ns task.deps
  (:require
   [task.util :refer [clj]]))

(def antq-base-cmd
  ;; nice use of `pr-str`
  [:-Sdeps (pr-str {:deps {'antq/antq {:mvn/version "1.6.2"}}}) :-M :-m :antq.core])

;; call the function with
;; bb -m task.deps/check-deps
#_:clj-kondo/ignore
(defn check-deps
  []
  (let [cmd antq-base-cmd]
    (clj cmd)
    nil))

(defn fix-deps
  []
  (let [cmd (concat antq-base-cmd [:--upgrade :--force])]
    (clj cmd)))
