;; see docs:
;; https://github.com/babashka/process/blob/master/API.md#process
(ns task.util
  (:require
   ;; see https://github.com/babashka/process#clojurepprint
   [babashka.process :as p]
   [babashka.deps :refer [clojure]]
   [clojure.pprint :as pprint]
   [clojure.string :as str]))

;; see https://github.com/babashka/process#clojurepprint
;; pprint does not order between IPersistentMap and IDeref
(prefer-method pprint/simple-dispatch clojure.lang.IPersistentMap clojure.lang.IDeref)

(def ^:dynamic *cwd* nil)

(defn- format-duration
  [duration-ms]
  (let [duration-sec (/ duration-ms 1000.0)]
    (str duration-sec)))

(defn- check
  [proc]
  (try
    (p/check proc)
    (catch Exception _e
      (println "> command exited with non-zero code: " (:exit @proc)))))

(defn- launcher
  [launch-fn cmd-parser]
  (fn launch
    ([cmd]
     (launch cmd nil))
    ([cmd & {:keys [verbose] :as opts
             :or {verbose true}}]
     (let [opts (merge {:inherit true
                        :dir *cwd*
                        :shutdown p/destroy-tree}
                       (dissoc opts :verbose))
           cmd (cmd-parser cmd)
           start (System/currentTimeMillis)
           _ (and verbose (println "> command started:" (pr-str cmd)))
           proc (launch-fn cmd opts)
           ;; `babashka.deps/clojure` might return nil for `ps`.
           ret (and proc (check proc))
           end (System/currentTimeMillis)
           duration-sec (format-duration (- end start))
           _ (and verbose
                  (println (format "> command ended.  %s seconds elapsed." duration-sec)))]

       ;; exit in case child errs
       (when (and proc (not= 0 (:exit @proc)))
         (System/exit (:exit @proc)))

       ret))))

;; NB for qualified keyword/symbol, e.g., `:linux/amd64`,
;; `name` only returns `amd64`.
(def sh
  "The command `sh` is blocking. A custom option `verbose` (default true) is added to
   control output of timing."
  (launcher p/process (fn [cmd]
                        (cond
                          (sequential? cmd) (->> cmd (map (comp str symbol)) (str/join " "))
                          :else (-> cmd symbol str)))))

(comment
  (sh [:ls :-al])
  (sh "ls -al")
  (sh "bash -c \"env |grep http\""))

;; see https://book.babashka.org/master.html#_clojure
;; babashka already contains `clj`, so no need to spawn another `clj` process.
;; see `(doc clojure)` for the full options.
(def clj
  "`cmd` is a vector of arguments."
  (launcher clojure (fn [cmd]
                      (cond
                        (sequential? cmd) (into [] (map (comp str symbol)) cmd)
                        ;; split into a vector
                        :else (p/tokenize cmd)))))

(comment
  (p/tokenize "a \"b c\"")
  (clj ["-Stree"]))

(defn git-hash* []
  (str/trim (:out (sh "git rev-parse HEAD" {:out :string :verbose false}))))

(def git-hash-6 (memoize #(subs (git-hash*) 0 6)))
