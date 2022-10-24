(ns fpl.clj.concurrent
  (:import
   [java.util.concurrent
    Executors
    ExecutorService
    ThreadPoolExecutor
    ForkJoinPool
    ForkJoinPool$ForkJoinWorkerThreadFactory
    ForkJoinWorkerThread
    RejectedExecutionException
    RejectedExecutionHandler
    TimeUnit
    ThreadFactory
    ArrayBlockingQueue
    Future]
   [java.util.concurrent.atomic
    AtomicLong]))

;; see also https://github.com/tolitius/lasync/blob/master/src/lasync/core.clj
(defonce available-cores
  (.. Runtime getRuntime availableProcessors))

(defn- rejected-handler
  []
  (reify RejectedExecutionHandler
    (rejectedExecution [_ runnable executor]
      (println "executor cannot run task" :runnable runnable)
      (throw (RejectedExecutionException. (str "Rejected execution: " runnable))))))

(defn thread-factory ^ThreadFactory
  [prefix ^AtomicLong counter]
  (reify ThreadFactory
    (newThread [_ runnable]
      (doto (Thread. runnable)
        (.setDaemon true)
        (.setName (format "%s-%s" prefix (.getAndIncrement counter)))))))

(comment
  ;; to see all threads
  (.. Thread getAllStackTraces keySet))

;; this thread pool is a cached thread pool as idle threads stay some time before being recycled.
;; see https://www.baeldung.com/java-executors-cached-fixed-threadpool for different implementations/parameters
;; of thread pool based on `j.u.c.ThreadPoolExecutor`.
(defn thread-pool-executor
  ([]
   (thread-pool-executor {}))
  ([{:keys [core-pool-size max-pool-size keep-alive-ms queue-capacity thread-prefix]
     :or {core-pool-size available-cores
          max-pool-size (int (* 2 core-pool-size))
          keep-alive-ms 30000
          queue-capacity 1000
          thread-prefix "fpl-"}}]
   (let [counter (AtomicLong. 0)]
     (doto (ThreadPoolExecutor. core-pool-size
                                max-pool-size
                                keep-alive-ms
                                TimeUnit/MILLISECONDS
                                (ArrayBlockingQueue. queue-capacity)
                                (thread-factory thread-prefix counter)
                                (rejected-handler))
      ;; see https://is.gd/hizike
      ;; the executor will automatically be reclaimed if:
      ;; all threads die and the executor is no longer referenced.
       (.allowCoreThreadTimeOut true)))))

(defn fork-join-thread-factory ^ForkJoinPool$ForkJoinWorkerThreadFactory
  [prefix counter]
  (reify ForkJoinPool$ForkJoinWorkerThreadFactory
    (newThread [_ pool]
      (let [^ForkJoinWorkerThread thread (.newThread ForkJoinPool/defaultForkJoinWorkerThreadFactory pool)
            thread-name (format "%s-%s" prefix (.getAndIncrement counter))]
        (.setName thread thread-name)
        thread))))

;; see https://www.pluralsight.com/guides/introduction-to-the-fork-join-framework#module-understandingtheframeworkclasses 
;; fork-join pool only runs `ForkJoinTask`s.  the benefit of fj is its work-stealing mechanism.
(defn fork-join-pool
  ;; `parallelism` is actuall the number of worker threads.
  ;; see https://stackoverflow.com/a/14301916/855160
  [{:keys [parallelism thread-prefix]
    :or {parallelism available-cores
         thread-prefix "fpl"}}]
  (let [counter (AtomicLong. 0)]
    (doto (ForkJoinPool. parallelism (fork-join-thread-factory thread-prefix counter) nil false))))

;; it is advised to reuse the default pool to reduce overheads instead of creating a new extra fj pool.
(defn fork-join-common-pool
  []
  (ForkJoinPool/commonPool))

(defn scheduled-thread-pool
  ([]
   (scheduled-thread-pool {}))
  ([{:keys [core-pool-size]
     :or {core-pool-size 1}}]
   (Executors/newScheduledThreadPool core-pool-size)))

(defn shutdown
  [^ExecutorService executor]
  (when executor
    (.shutdown executor)))

(defn shutdown-now
  [^ExecutorService executor]
  (when executor
    (.shutdownNow executor)))

(defn submit-task ^Future
  [^ExecutorService executor ^Runnable f]
  ;; see https://stackoverflow.com/a/3986509/855160 for difference on `.execute` and `.submit`.
  (.submit executor f))

(comment
  (def *executor (thread-pool-executor nil))

  (-> (submit-task *executor (fn [] (println "GOGO")))
      .get))

(comment
  (scheduled-thread-pool 3))

(defmacro completable-future
  "Return a CompletableFuture that will evaluate the body asynchronously.
  Can be deref'd, future-cancel'd, etc. Call then (below) on this to provide
  a function to call when the future completes."
  [& body]
  `(java.util.concurrent.CompletableFuture/supplyAsync
    (reify java.util.function.Supplier
      (~'get [_#] ~@body))))

(defmacro then-apply
  "Given a CompletableFuture and a function, when the future completes,
  invoke the function on its result."
  [cf f]
  `(.thenApply
    ~cf
    (reify java.util.function.Function
      (~'apply [_# v#] (~f v#)))))

(comment
  (let [fut (completable-future
             (Thread/sleep 1000)
             (println "hello world"))]
    (then-apply fut prn)))

(defmacro exceptionally
  "Given a CompletableFuture and a function, if the future completes
  with an exception, invoke the function on that exception."
  [cf f]
  `(.exceptionally
    ~cf
    (reify java.util.function.Function
      (~'apply [_# v#] (~f v#)))))

(defmacro handle
  "Given a CompletableFuture and a function, invoke the function with the result and exception if any.
   - `f`: A function whose arglist is [ret ex]."
  [cf f]
  `(.handle
    ~cf
    (reify java.util.function.BiFunction
      (~'apply [_# v# ex#] (~f v# ex#)))))

(comment
  (let [fut (completable-future
             (Thread/sleep 1000)
             (println "hello world")
             (if (rand-nth [true false])
               42
               (throw (ex-info "something wrong" {}))))
        fun (fn [ret ex]
              (prn ret ex))]
    (handle fut fun)))
