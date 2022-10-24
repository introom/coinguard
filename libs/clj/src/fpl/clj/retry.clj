(ns fpl.clj.retry)

(defn retry
  "The `opts` map accepts:
   - `:max-retries`: maximum number of retries. `-1` means infinite retries.
   - `:backoff-ms`:  an infinite sequence of delays in ms"
  [f & {:keys [max-retries backoff-ms]
        :as opts}]
  {:pre [(int? max-retries) (some? backoff-ms)]}
  (let [infinite-sentinel -1]
    (loop [retry-cnt 1
           [delay-ms & backoff] backoff-ms]
      (let [ret (try (f)
                     (catch Exception e e))]
        (if (instance? Throwable ret)
          (if (and (not= infinite-sentinel max-retries)
                   (>= retry-cnt max-retries))
            (throw ret)
            (do
              (Thread/sleep delay-ms)
              (recur (inc retry-cnt)
                     backoff)))
          ret)))))

(comment
  ;; linear backoff
  (retry (fn []
           (println "hi")
           (throw (ex-info "something wrong!" {})))
         :max-retries 3
         :backoff-ms (repeat 1000))

  ;; exponential backoff
  (retry (fn []
           (println "hi")
           (throw (ex-info "something wrong!" {})))
         :max-retries 3
         :backoff-ms (iterate (partial * 2) 1000)))