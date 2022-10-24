(ns task.hello
  (:require
   [fpl.clj.process :refer [sh]]
   [fpl.clj.template :refer [render-path]]))

(defn kube-install-hello-cmd
  [ctx]
  (let [content (doto (render-path "task/hello/hello.yml" ctx) println)]
    (sh "kubectl apply -f -" {:in content})))