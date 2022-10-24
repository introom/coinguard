;; see docs:
;; https://github.com/babashka/process/blob/master/API.md#process
(ns fpl.clj.process
  (:require
   [babashka.process :as p]
   [environ.core :as environ]
   [fpl.clj.time :as time]
   [fpl.clj.bb :refer [if-bb]]
   [clojure.pprint :as pprint]
   [clojure.string :as str]))

(def environ environ/env)

(defn get-env
  "Throws an exception if the environment variable is not found
   unless `not-found` is specified."
  ([kw]
   (when-not (contains? environ kw)
     (throw (Exception. (format "Environment variable %s not found." kw))))
   (environ kw))
  ([kw not-found]
   (if (contains? environ kw)
     (environ kw)
     not-found)))

;; see https://github.com/babashka/process#clojurepprint
;; pprint does not order between IPersistentMap and IDeref
(prefer-method pprint/simple-dispatch clojure.lang.IPersistentMap clojure.lang.IDeref)

(defn- format-duration
  [duration-ms]
  (-> duration-ms time/ms->sec str))

(defn- check
  [proc]
  (try
    (p/check proc)
    (catch Exception _e
      (println "> Command exited with non-zero code: " (:exit @proc)))))

(defn- cmd-str
  [cmd max-cmd-length]
  (let [cmd (print-str cmd)]
    (if (or (= -1 max-cmd-length)
            (>= max-cmd-length (count cmd)))
      cmd
      (str (subs cmd 0 max-cmd-length) " ..."))))

(defn launch-fn
  "The returned function `launch` waits till the child process exists."
  [f cmd-parser]
  (fn launch
    ([cmd]
     (launch cmd nil))
    ;; `max-cmd-length` caps the printing of command name.  -1 means no cap.
    ([cmd & {:keys [verbose max-cmd-length] :as opts
             :or {verbose true max-cmd-length -1}}]
     (let [opts (merge {:inherit true
                        ;; nil means the current working directory
                        :dir nil
                        :shutdown p/destroy-tree}
                       (dissoc opts :verbose))
           cmd (cmd-parser cmd)
           start (time/epoch-ms)
           _ (and verbose (println "> Command started:" (cmd-str cmd max-cmd-length)))
           proc (f cmd opts)
           ;; `babashka.deps/clojure` might return nil for `ps`.
           ret (and proc (check proc))
           end (time/epoch-ms)
           duration-sec (format-duration (- end start))
           _ (and verbose
                  (printf "> Command ended.  %s seconds elapsed.\n" duration-sec))]

       (when (and proc (not= 0 (:exit @proc)))
         (throw (ex-info "Command failed." {:proc @proc :exit (:exit @proc)})))

       ret))))

;; NB for qualified keyword/symbol, e.g., `:linux/amd64`,
;; `name` only returns `amd64`.
(def sh
  "```
   (sh cmd opts)
   ```
   `cmd` is is either a vector or a string or a single string that will be tokenized."
  (launch-fn p/process (fn [cmd]
                         (cond
                           (sequential? cmd) (->> cmd (map (comp str symbol)) (str/join " "))
                           :else (-> cmd symbol str)))))
(comment
  (sh [:ls :-al])
  (sh "ls -al")
  (sh "bash -c \"env |grep http\""))

;; see https://book.babashka.org/master.html#_clojure
;; the benefit of `clojure` from babashka is it will avoid spawning a new process (best effort)
;; see `(doc clojure)` for the full options.
(defn- clojure-fn []
  (if-bb
   (requiring-resolve 'babashka.deps/clojure)
   (fn [cmd opts]
     (p/process (concat ["clojure"] cmd) opts))))

(def clj
  "Example invocation:
   ```
  (clj [\"-Stree\"])
  (clj \"-Stree\")
   ```"
  (launch-fn (clojure-fn) (fn [cmd]
                            (cond
                              (sequential? cmd) (into [] (map (comp str symbol)) cmd)
                               ;; split into a vector
                              :else (p/tokenize cmd)))))

(comment
  (p/tokenize "a \"b c\"")
  (clj "-Stree")
  (clj ["-Stree"])
  (clj ["-Stree"] {:verbose false}))
