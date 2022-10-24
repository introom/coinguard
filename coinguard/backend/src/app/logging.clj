(ns app.logging
  (:require [clojure.tools.logging :as log]
            [clojure.tools.logging.impl :as impl]))

;; we *explicitly* set slf4j as the logging facade
;; see https://clojure.github.io/tools.logging/#clojure.tools.logging/*logger-factory*
;; and (impl/find-factory) which implicitly first checks slf4j
(alter-var-root #'log/*logger-factory* (constantly (impl/slf4j-factory)))
;; we use logback as the backend for slf4j

(defmacro debug [msg & args]
  `(log/debug :line ~(:line (meta &form)) :msg (pr-str ~msg) ~@args))

(defmacro info [msg & args]
  `(log/info :line ~(:line (meta &form)) :msg (pr-str ~msg) ~@args))

(defmacro warn [msg & args]
  `(log/warn :line ~(:line (meta &form)) :msg (pr-str ~msg) ~@args))

(defmacro error [msg & args]
  `(do
     ;; also set *e to be error under current namespace
     ;; NB given `(:err (Exception.))`, this will evaluate `(Exception.)` here
     ;; and also in the real `log/error` part.
     (when (= (System/getProperty "dev") "true")
       (let [m# (hash-map ~@args)]
         (when-some [err# (:err m#)]
           ;; we could tap instead.
           (alter-var-root #'*e (constantly err#)))))
     (log/error :line ~(:line (meta &form)) :msg (pr-str ~msg) ~@args)))

(defmacro spy [expr]
  `(let [val# ~expr]
     (log/debug :line ~(:line (meta &form)) :msg (pr-str "spy on form") :val val#)
     val#))

(comment
  (error "hi" :err (ex-info "Exception" {}))
  (info (str "hi"))
  (macroexpand-1 '(info (str "hi")))
  (macroexpand-1 '(error "hi" :err (ex-info "Exception" {}))))