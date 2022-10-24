(ns fpl.clj.async
  "Conventionally, this namespace is required as `aa`."
  (:require
   [clojure.core.async :as a])
  (:import
   java.util.concurrent.Executor))

(defn batch-ch
  "Returns a channel of `{:data [items] :status :batch}` where item is accumulated
   from the input channel."
  [in-ch {:keys [batch-size
                 batch-timeout-ms
                 buffer-size]
          :or {batch-size 128
               batch-timeout-ms (* 30 1000)
               buffer-size (* batch-size 2)}}]
  (let [out-ch (a/chan buffer-size)]
    (a/go-loop [timeout-ch (a/timeout batch-timeout-ms)
                buf #{}]
      (a/alt!
        timeout-ch (do
                     (a/>! out-ch
                           {:data buf
                            :status :timeout})
                     (recur (a/timeout batch-timeout-ms)
                            ;; start from scratch
                            #{}))
        in-ch ([val]
               (if (nil? val)
                 (do
                   (a/offer! out-ch {:data buf
                                     :status :batch})
                   (a/close! out-ch))
                 (let [buf (conj buf val)]
                   (if (= (count buf) batch-size)
                     (do
                       (a/>! out-ch {:data buf
                                     :status :batch})
                       (recur (a/timeout batch-timeout-ms)
                              #{}))
                     (recur timeout-ch
                            buf)))))))
    out-ch))

;; different to `core.async/thread-call`, we allow specifying the executor.
(defn thread-call
  [^Executor executor f]
  (let [ch (a/chan 1)]
    (try
      (.execute executor
                (fn []
                  (try
                    (let [ret (try (f)
                                   (catch Exception e
                                     e))]
                      (when (some? ret)
                        (a/>!! ch ret)))
                    (finally
                      (a/close! ch)))))
      ch
      (catch java.util.concurrent.RejectedExecutionException _e
        (a/close! ch)
        ch))))

(defmacro thread
  {:style/indent 1}
  [^Executor executor & body]
  `(thread-call ~executor (^:once fn* [] ~@body)))

(comment
  (require '[fpl.clj.concurrent :refer [thread-pool-executor]])
  (thread (thread-pool-executor) (println "asdf")))

(defmacro with-close
  [ch & body]
  `(try
     ~@body
     (finally
       (some-> ~ch a/close!))))

(comment
  (let [done-ch (a/chan)]
    (with-close done-ch)
    (a/<!! done-ch)))

(defn thread-sleep
  [ms]
  (Thread/sleep ms))
