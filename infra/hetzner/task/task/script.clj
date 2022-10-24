(ns task.script
  (:require
   [fpl.clj.template :as tmpl]))

(def install-deps (partial tmpl/render-path "task/script/install-deps.sh"))

(def install-k3s-controller (partial tmpl/render-path "task/script/install-k3s-controller.sh"))
(def install-k3s-worker (partial tmpl/render-path "task/script/install-k3s-worker.sh"))
