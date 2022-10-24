(ns task.lint
  (:require
   [task.util :refer [clj]]
   [task.format :refer [check-fmt]]))

(def kondo-base-cmd
  (str "-Sdeps '{:deps {clj-kondo/clj-kondo  {:mvn/version \"2022.04.25\"}}}'"
       " -M -m clj-kondo.main"
       " --config .clj-kondo/config.edn"
       " --lint dev/ src/ test/"))

(defn- lint-kondo
  []
  (let [cmd kondo-base-cmd]
    (clj cmd)))

(defn lint
  []
  ;; the sys will exit if command fails.
  (check-fmt)
  (lint-kondo))
  
  