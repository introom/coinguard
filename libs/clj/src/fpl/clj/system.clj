(ns fpl.clj.system
  (:require
   [integrant.core :as ig]
   [fpl.clj.logging :as log]
   [clojure.core.async :as a])
  (:import
   (java.util.concurrent Phaser TimeUnit)))

;; controller for the whole app
(defmethod ig/init-key ::ctl
  [_ ctx]
  (let [-close-ch (a/chan)
        close-mult (a/mult -close-ch)]
    {:-close-ch -close-ch
     :close-mult close-mult
     :close-ch-fn #(a/tap close-mult (a/chan))
     :close-phaser (Phaser. 1)}))

(defmethod ig/halt-key! ::ctl
  [_ ctx]
  (let [{:keys [-close-ch close-phaser wait-ms]
         :or {wait-ms 10000}} ctx]
    (a/close! -close-ch)
    (try
      ;; we initialized the phaser with =1=, otherwise the =phaser= will wait timeout.
      (.arriveAndDeregister close-phaser)
      (.awaitAdvanceInterruptibly close-phaser (.getPhase close-phaser) wait-ms TimeUnit/MILLISECONDS)
      (catch Exception e
        (log/debug "go blocks timeout")))))

(defonce system nil)
(defonce config nil)

(defn setup
  [m]
  (alter-var-root #'config (constantly m)))

(defn start
  []
  (ig/load-namespaces config)
  (alter-var-root #'system
                  (fn [sys]
                    (when sys (ig/halt! sys))
                    (-> config
                        ig/prep
                        ig/init)
                    (log/info "system started."))))

(defn stop
  []
  (alter-var-root #'system
                  (fn [sys]
                    (when sys
                      (log/info "system stopped.")
                      (ig/halt! sys)))))

(defn run
  [& _args]
  (start)
  ;; pattern.
  ;; see https://github.com/duct-framework/core/blob/2816b7273011ca0d119141210fb7fe1c9599176b/src/duct/core.clj#L28
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn [] (ig/halt! system))))
  ;; keep running forever
  @(promise))


(comment
  (log/info "go")
  )