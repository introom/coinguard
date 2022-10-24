(ns app.task.worker
  (:require
   [app.logging :as log]
   [app.util.async :as aa]
   [app.util.time :as time]
   [app.config :as config]
   [app.db :as db]
   [integrant.core :as ig]
   [honey.sql.helpers :as h]
   [honey.sql :as sql]
   [clojure.core.async :as a])
  (:import
   [java.util.concurrent
    ExecutorService]))

(declare event-loop-fn)

;; `worker` periodically runs `event-loop-fn` in a thread, waits for it
;;  and continue.
(defmethod ig/init-key ::worker
  [_ {{:keys [close-mult phaser]} :ctrl
      :keys [db executor]
      :as ctx}]
  (log/info "worker has started")
  (when-not (= (config/get :c/env) :local)
    (let [delay-ms 5000
          concurrency 1
          queue :default
          batch-size 1
          close-ch (a/tap close-mult (a/chan))]
      (dotimes [idx concurrency]
        (.register phaser)
        (a/go-loop []
          (let [event-ch (event-loop-fn {:db db 
                                         :executor executor
                                         :queue queue
                                         :batch-size batch-size})
                [val port] (a/alts! [close-ch event-ch] :priority true)]
            (cond
              (= close-ch port)
              (do
                (.arriveAndDeregister phaser)
                (log/info "exit worker go block" :idx idx))

              (= ::handled val)
              (do
                (log/debug "task handled" :val val)
                (recur))

              (= ::empty val)
              (do
                (log/debug "no task found")
                (a/<! (a/timeout delay-ms))
                (recur))

              (instance? Exception val)
              (do
                (log/error "find exception" :err val)
                (a/<! (a/timeout delay-ms))
                (recur))

              (nil? val)
              (do
                ;; this shouldn't happen
                (log/error "function returns nil")
                (a/<! (a/timeout (* 10 delay-ms)))
                (recur))

              :else
              (do
                (log/error "unknown result" :val val :port port)
                (recur)))))))))

(defmethod ig/halt-key! ::worker
  [_ ctx]
  ;; the go blocks are already closed by the global mult.
  (log/info "worker has stopped"))

(defn- sql:read-next-tasks
  [queue batch-size]
  (-> (h/select :*)
      (h/from :task)
      (h/where [:<= :scheduled_at :%now]
               [:= :queue (name queue)]
               [:or [:= :status "new"] [:= :status "retry"]])
      (h/order-by [:priority :desc] [:scheduled_at :asc])
      (h/limit batch-size)
      (h/for [:update :skip-locked])
      sql/format))

(defn- select-next-tasks
  [db {:keys [queue batch-size]}]
  (let [queue (name queue)]
    (db/execute db (sql:read-next-tasks queue batch-size))))

(defn- decode-task-row
  [{:keys [props name] :as row}]
  (when row
    (cond-> row
      ;;  XXX TODO, use schema to coerece "eth" to :eth
      (db/pgobject? props) (assoc :props (db/decode-json-pgobject props))
      (string? name)       (assoc :name (keyword name)))))

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
              (if (< (:retry-num task) (:max-retries task))
                (merge {:status :retry :task task :error ex}
                       (select-keys edata [:schedule-delay :retry-inc]))
                {:status :failed :task task :error ex})))]
    (try
      (handle-task)
      (catch Exception e
        (handle-exception e)))))

(defn- submit-task
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
  [{:keys [id error retry-inc schedule-delay]}]
  (-> (h/update :task)
      (h/set {:scheduled_at [:+ :%clock_timestamp [:cast schedule-delay :interval]]
              :modified_at :%clock_timestamp
              :error error
              :status "retry"
              :retry_num [:+ :retry_num retry-inc]})
      (h/where [:= :id id])
      sql/format))

(defn- mark-as-retry
  [conn {:keys [task ex retry-inc schedule-delay]
         :or {schedule-delay (time/duration {:seconds 10})
              retry-inc 1}}]
  (db/execute-one conn (sql:mark-as-retry {:id (:id task)
                                           :error (ex-message ex)
                                           :retry-inc retry-inc
                                           :schedule-delay (db/interval schedule-delay)})))

(defn- sql:mark-as-failed
  [{:keys [id error]}]
  (-> (h/update :task)
      (h/set {:modified-at :%now
              :error error
              :status "failed"})
      (h/where [:= :id id])
      sql/format))

(defn- mark-as-failed
  [conn {:keys [task ex]}]
  (db/execute-one conn (sql:mark-as-failed {:id (:id task)
                                            :error (ex-message ex)})))

(defn- sql:mark-as-completed
  [{:keys [id]}]
  (-> (h/update :task)
      (h/set {:modified-at :%now
              :completed-at :%now
              :status "completed"})
      (h/where [:= :id id])
      sql/format))

(defn- mark-as-completed
  [conn {:keys [task]}]
  (db/execute-one conn (sql:mark-as-completed {:id (:id task)})))

(defn- event-loop-fn*
  [{_db :db
    :keys [executor batch-size queue]
    :as ctx}]
  (db/with-transaction [conn _db]
    (let [tasks (->> (select-next-tasks conn {:queue queue :batch-size batch-size})
                     (map decode-task-row)
                     (seq))
          ctx  (assoc ctx :db conn)]
      (log/debug "found tasks" :tasks tasks)
      (if (nil? tasks)
        ::empty

        (let [proc-xf (comp (map #(partial run-task ctx %))
                            (map #(submit-task executor %)))]
          (->> (into [] proc-xf tasks)
               (map deref)
               (run! (fn [ret]
                       (case (:status ret)
                         :retry (mark-as-retry conn ret)
                         :failed (mark-as-failed conn ret)
                         :completed (mark-as-completed conn ret)))))
          ::handled)))))

(comment
  (db/with-transaction [conn (:app.db/db user/system)]
    (let [items (->> (select-next-tasks conn {:queue :default :batch-size 3})
                     (map decode-task-row)
                     (seq))
          ctx  (assoc {} :db conn)]
      (def *items items))))

(defn- event-loop-fn
  [{:keys [executor] :as ctx}]
  (aa/thread-call executor #(event-loop-fn* ctx)))

(defn- sql:create-task
  [{:keys [name props queue priority schedule-delay max-retries]}]
  (-> (h/insert-into :task)
      (h/values [{:name name
                  :props props
                  :queue queue
                  :priority priority
                  :scheduled-at [:+ :%clock_timestamp schedule-delay]
                  :max-retries max-retries}])
      (h/returning :*)
      sql/format))

(defn- db:create-task
  [db {:keys [name props queue priority schedule-delay max-retries] :as task}]
  (let [schedule-delay (-> schedule-delay time/duration db/interval)
        props (db/json props)
        task (assoc task :props props :schedule-delay schedule-delay)]
    (db/execute-one db (sql:create-task task))))

(defn submit
  [db {:keys [name props queue priority ^{:unit :ms} schedule-delay max-retries]
       :or {schedule-delay 5000 queue :default priority 100 max-retries 3}}]
  (db:create-task db {:name name
                      :props props
                      :queue (clojure.core/name queue)
                      :priority priority
                      :max-retries max-retries
                      :schedule-delay schedule-delay}))

(comment
  (submit (:app.db/db user/system)
          {:name "mobile"
           :props {:a :b}
           :queue :default
           :priority 30
           :schedule-delay 5000
           :max-retries 4}))

(comment
  (a/thread-call (fn [] (throw (Exception. "ASDF")))))