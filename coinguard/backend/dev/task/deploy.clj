(ns task.deploy
  (:require
   [task.util :refer [sh]]
   [task.image :refer [image-name]]))

(defn env-name []
  (let [env (System/getenv "APP_ENV")]
    (if env
      env
      (throw (ex-info "APP_ENV is not set" {})))))

;; see `kubectl set image --help`
(defn deploy
  []
  (let [resource-name (format "coinguard-%s-api" (env-name))
        cmd
        (format "kubectl -n coinguard set image statefulset/%s %s=%s"
                resource-name resource-name (image-name))]
    (println "execute command:" cmd)
    (sh cmd)))

(comment
  (deploy))
