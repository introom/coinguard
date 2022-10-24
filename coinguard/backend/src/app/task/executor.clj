(ns app.task.executor
  (:require
   [app.logging :as log]
   [integrant.core :as ig])
  (:import
   [java.util.concurrent
    Executors
    ThreadPoolExecutor
    RejectedExecutionException
    RejectedExecutionHandler
    TimeUnit
    ArrayBlockingQueue]))

(defn- rejected-handler
  []
  (reify RejectedExecutionHandler
    (rejectedExecution [_ runnable executor]
      (log/error "executor cannot run task" :runnable runnable)
      (throw (RejectedExecutionException. (str "Rejected execution: " runnable))))))

;; see also https://github.com/tolitius/lasync/blob/master/src/lasync/core.clj
(defonce available-cores
  (.. Runtime getRuntime availableProcessors))

(defmethod ig/init-key ::executor
  [_ _ctx]
  (let [core-pool-size available-cores
        max-pool-size (int (* 2 core-pool-size))
        keep-alive-ms 10000
        queue-capacity 1000]
    (doto (ThreadPoolExecutor. core-pool-size
                               max-pool-size
                               keep-alive-ms
                               TimeUnit/MILLISECONDS
                               (ArrayBlockingQueue. queue-capacity)
                               (Executors/defaultThreadFactory)
                               (rejected-handler))
      ;; see https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html#:~:text=tasks%20become%20cancelled.-,Finalization,-A%20pool%20that
      ;; the executor will automatically be reclaimed if:
      ;; all threads die and the executor is no longer referenced.
      (.allowCoreThreadTimeOut true))))
(defmethod ig/halt-key! ::executor
  [_ executor]
  (when executor
    (.shutdownNow executor)))

(comment
  (do (def *system (ig/init
                    @(requiring-resolve 'app.main/system-config) [::executor]))
      (def *executor (::executor *system))))