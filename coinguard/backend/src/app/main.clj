(ns app.main
  (:require
   [app.runtime]
   [integrant.core :as ig]
   [clojure.core.async :as a]
   [app.logging :as log]
   [app.config :as config]
   [app.session :as session]
   [app.task.executor :as task.executor]
   [app.task.worker :as task.worker]
   [app.task.handler :as task.handler]
   [app.http.server :as server]
   [app.db :as db]
   [app.indexer.eth :as eth])
  (:import
   (java.util.concurrent Phaser TimeUnit))
  (:gen-class))

;; controller for the whole app
(defmethod ig/init-key ::ctrl
  [_ ctx]
  (let [-close-ch (a/chan)]
    {:-close-ch -close-ch
     :close-mult (a/mult -close-ch)
     :phaser (Phaser.)}))

(defmethod ig/halt-key! ::ctrl
  [_ ctx]
  (let [{:keys [-close-ch phaser]} ctx
        wait-ms 10000]
    (a/close! -close-ch)
    (try
      (.awaitAdvanceInterruptibly phaser (.getPhase phaser) wait-ms TimeUnit/MILLISECONDS)
      (catch Exception e
        (log/debug "go blocks timeout")))))

(def system-config
  {::ctrl nil
   ::db/db nil

   ::task.executor/executor nil
   ::task.handler/handler nil
   ::task.worker/worker
   {:ctrl (ig/ref ::ctrl)
    :db (ig/ref ::db/db)
    :executor (ig/ref ::task.executor/executor)
    :handler (ig/ref ::task.handler/handler)}

   ::session/daemon
   {:db (ig/ref ::db/db)
    :executor (ig/ref ::task.executor/executor)}

   ::server/server
   {:db (ig/ref ::db/db)
    :session (ig/ref ::session/daemon)}

   ::eth/eth
   {:ctrl (ig/ref ::ctrl)
    :db (ig/ref ::db/db)}})

(def system nil)

(defn- start []
  (ig/load-namespaces system-config)
  (alter-var-root #'system (fn [sys]
                             (when sys (ig/halt! sys))
                             (-> system-config
                                 ig/prep
                                 ig/init)))
  (log/info "welcome to coinguard"
            :version (config/get :version)))

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- stop []
  (alter-var-root #'system (fn [sys]
                             (when sys
                               (ig/halt! sys)))))

(defn -main
  [& _args]
  (start)
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread.  (fn [] (ig/halt! system))))
  ;; keeep running forever
  @(promise))

(comment
  (tap> system-config))