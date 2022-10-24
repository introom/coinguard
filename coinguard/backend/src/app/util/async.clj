(ns app.util.async
  (:require
   [clojure.core.async :as a])
  (:import
   java.util.concurrent.Executor))

(defn batch-input
  [in {:keys [batch-size
              batch-timeout-ms
              buffer-size]
       :or {batch-size 128
            batch-timeout-ms (* 30 1000)
            buffer-size (* batch-size 2)}
       :as opts}]
  (let [out (a/chan buffer-size)]
    (a/go-loop [timeout (a/timeout batch-timeout-ms)
                buf #{}]
      (a/alt!
        timeout (do
                  (a/>! out
                        {:data buf
                         :reason :timeout})
                  (recur (a/timeout batch-timeout-ms)
                         ;; start from scratch
                         #{}))
        in ([val]
            (if (nil? val)
              (do
                (a/offer! out {:data buf
                               :reason :batch})
                (a/close! out))
              (let [buf (conj buf val)]
                (if (= (count buf) batch-size)
                  (do
                    (a/>! out {:data buf
                               :reason :batch})
                    (recur (a/timeout batch-timeout-ms)
                           #{}))
                  (recur timeout
                         buf)))))))
    out))

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
  [executor & body]
  (if (= executor ::default)
    `(a/thread-call (^:once fn* [] (try ~@body (catch Exception e# e#))))
    `(thread-call ~executor (^:once fn* [] ~@body))))