(ns fpl.chrono.worker
  (:require
   [clojure.core.async :as a]
   [fpl.clj.logging :as log]
   [fpl.clj.async :as aa]
   [fpl.clj.db :as db]
   [fpl.clj.spec :as s]
   [honey.sql.helpers :as h]
   [honey.sql :as sql])
  (:import
   [java.lang
    AutoCloseable]
   [java.util.concurrent
    ExecutorService]))

(declare run-event-loop)
(declare run-worker-loop)

(defn start-worker
  [{:keys [db executor poll-ms concurrency queue batch-size] :as ctx}]
  (log/info "worker has started")
  (let [close-ch (a/chan)]
    (run-worker-loop (assoc ctx :close-ch close-ch))
    (reify AutoCloseable
      (close [_]
        (a/close! close-ch)))))

(defn stop-worker
  [^AutoCloseable w]
  (.close w)
  (log/info "worker has stopped."))

;; `run-worker-loop` periodically runs `run-event-loop`, which performs the execution in another thread.
(defn- run-worker-loop
  [{:keys [db executor poll-ms concurrency queue batch-size close-ch]
    :or {poll-ms 5000
         concurrency 1
         queue "default"
         batch-size 1}}]
  (dotimes [idx concurrency]
    (a/go-loop []
      (let [event-ch (run-event-loop {:db db
                                      :executor executor
                                      :queue queue
                                      :batch-size batch-size})
            [val port] (a/alts! [close-ch event-ch] :priority true)]
        (cond
          (= close-ch port)
          (do
            (log/info "exit worker go block" :go-loop-idx idx)
            nil)

          (= ::handled val)
          (do
            (log/debug "task handled" :val val)
            (recur))

          (= ::empty val)
          (do
            (log/debug "no task found")
            (a/<! (a/timeout poll-ms))
            (recur))

          (instance? Exception val)
          (do
            (log/error "found exception" :error val)
            (a/<! (a/timeout poll-ms))
            (recur))

          (nil? val)
          (do
            ;; this shouldn't happen
            (log/error "function returned nil")
            (a/<! (a/timeout (* 10 poll-ms)))
            (recur))

          :else
          (do
            (log/error "unknown result" :val val :port port)
            (recur)))))))

(def ^:private task-pending-status "pending")
(def ^:private task-retry-status "retry")
(def ^:private task-failed-status "failed")
(def ^:private task-completed-status "completed")

(defn- sql:select-next-tasks
  [queue batch-size]
  (-> (h/select :*)
      (h/from :chrono-task)
      (h/where [:<= :scheduled_at :%now]
               [:= :queue queue]
               [:or [:= :status task-pending-status] [:= :status task-retry-status]])
      (h/order-by [:priority :desc] [:scheduled_at :asc])
      (h/limit batch-size)
      (h/for [:update :skip-locked])
      sql/format))

(comment
  (sql:select-next-tasks "default" 1))

(defn- select-next-tasks
  [db {:keys [queue batch-size]}]
  (let [queue (-> queue symbol str)]
    (db/execute-many db (sql:select-next-tasks queue batch-size))))

(def s:decode-task-row
  ;; some coercion
  [:map
   [:props ]]
  )

(defn- decode-task-row
  [{:keys [props name] :as row}]
  (assert row)
  (def *r row)
  (cond-> row
    (db/pgobject? props) (assoc :props (db/decode-json props))
    (string? name)       (assoc :name (keyword name))))

(defn- run-task
  [{:keys [handler] :as ctx}
   {:keys [name] :as task}]
  (log/debug "start to run task." :task task)
  (letfn [(handle-task
            []
            (log/debug "run task" :task task)
            (let [handler (get handler name)]
              (if handler
                (handler task)
                (do
                  (log/error "missing task handler" :name name)
                  {:status :failed :task task :error "Missing task handler."}))
              {:status :completed :task task}))

          (handle-exception
            [ex]
            (log/debug "exception happend in running task" :ex ex)
            (let [edata (ex-data ex)]
              (if (< (:retry-cnt task) (:max-retries task))
                (merge {:status :retry :task task :error ex}
                       (select-keys edata [:poll-ms :retry-inc]))
                {:status :failed :task task :error ex})))]
    (try
      (handle-task)
      (catch Exception e
        (handle-exception e)))))

(defn- submit-executor-task
  [^ExecutorService executor handler]
  (let [ret (promise)]
    (log/debug "submit task")
    (try
      (.submit executor ^Runnable #(deliver ret (handler)))
      (catch Exception e
        (log/error "failed to submit task" :ex e)))
    (log/debug "returned value" :val @ret)
    ret))

;; see cast
;; https://cljdoc.org/d/com.github.seancorfield/honeysql/2.2.861/doc/getting-started/sql-special-syntax-#cast
;; XXX though I feel the cast is not necessary.
(defn- sql:mark-as-retry
  [{:keys [id error retry-inc poll-ms]}]
  (-> (h/update :chrono-task)
      (h/set {:scheduled_at [:+ :%clock_timestamp [:cast poll-ms :interval]]
              :modified_at :%clock_timestamp
              :error error
              :status task-retry-status
              :retry_num [:+ :retry_num retry-inc]})
      (h/where [:= :id id])
      sql/format))

(defn- mark-as-retry
  [conn {:keys [task ex retry-inc poll-ms]
         :or {poll-ms 10000
              retry-inc 1}}]
  (db/execute-one conn (sql:mark-as-retry {:id (:id task)
                                           :error (ex-message ex)
                                           :retry-inc retry-inc
                                           :poll-ms (db/interval poll-ms)})))

(defn- sql:mark-as-failed
  [{:keys [id error]}]
  (-> (h/update :chrono-task)
      (h/set {:modified-at :%now
              :error error
              :status task-failed-status})
      (h/where [:= :id id])
      sql/format))

(defn- mark-as-failed
  [conn {:keys [task ex]}]
  (db/execute-one conn (sql:mark-as-failed {:id (:id task)
                                            :error (ex-message ex)})))

(defn- sql:mark-as-completed
  [{:keys [id]}]
  (-> (h/update :chrono-task)
      (h/set {:modified-at :%now
              :completed-at :%now
              :status task-completed-status})
      (h/where [:= :id id])
      sql/format))

(defn- mark-as-completed
  [conn {:keys [task]}]
  (db/execute-one conn (sql:mark-as-completed {:id (:id task)})))

(defn- event-loop-fn*
  [{:keys [db executor batch-size queue]
    :as ctx}]
  (db/with-transaction [conn db]
    (let [tasks (->> (select-next-tasks conn {:queue queue :batch-size batch-size})
                     (map decode-task-row)
                     seq)
          ctx  (assoc ctx :db conn)]
      (log/debug "found tasks" :tasks tasks)
      (if (nil? tasks)
        ::empty

        (let [proc-xf (comp (map #(partial run-task ctx %))
                            (map #(submit-executor-task executor %)))]
          (->> (into [] proc-xf tasks)
               (map deref)
               (run! (fn [ret]
                       (case (:status ret)
                         :retry (mark-as-retry conn ret)
                         :failed (mark-as-failed conn ret)
                         :completed (mark-as-completed conn ret)))))
          ::handled)))))

(comment
  (db/with-transaction [conn 'db]
    (let [items (->> (select-next-tasks conn {:queue "default" :batch-size 3})
                     (map decode-task-row)
                     (seq))
          ctx  (assoc {} :db conn)]
      (def *items items))))

(defn- run-event-loop
  [{:keys [executor] :as ctx}]
  (aa/thread executor (event-loop-fn* ctx)))

(defn- sql:create-task
  [{:keys [name props queue priority delay-ms max-retries]}]
  (-> (h/insert-into :chrono-task)
      (h/values [{:name name
                  :props props
                  :queue queue
                  :priority priority
                  :scheduled-at [:+ :%clock_timestamp delay-ms]
                  :max-retries max-retries}])
      (h/returning :*)
      sql/format))

(defn submit-task
  "- `delay-ms`: The delay from now to schedule the task."
  [db {:keys [queue name props priority delay-ms max-retries]
       :or {queue "default" delay-ms 0 priority 100 max-retries 3}}]
  (let [delay-ms (-> delay-ms db/interval)
        queue (-> queue symbol str)
        props (-> props db/encode-json)]
    (db/execute-one db (sql:create-task {:name name
                                         :props props
                                         :queue queue
                                         :priority priority
                                         :max-retries max-retries
                                         :delay-ms delay-ms}))))