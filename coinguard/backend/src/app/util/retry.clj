(ns app.util.retry)

(defmacro retry
  [opts & body]
  ;; NO ^:once because we are *re-running* the function
  `(retry* ~opts (fn* [] ~@body)))

(defmacro retry-default
  [& body]
  `(retry {} ~@body))

(defn retry*
  [{:keys [max-retries delay-ms]
    :or {max-retries 3
         delay-ms 1000}
    :as opts}
   f]
  (loop [retry-cnt 1]
    (let [ret (try (f) (catch Exception e e))]
      (if (instance? Throwable ret)
        (if (>= retry-cnt max-retries)
          (throw ret)
          (do
            (Thread/sleep delay-ms)
            (recur (inc retry-cnt))))
        ret))))