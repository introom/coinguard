;; log path: app -> clojure.tools.logging -> slf4j -> logback
(ns fpl.clj.logging
  (:require
   [clojure.tools.logging :as log]
   [fpl.clj.process :refer [get-env]]))

(defmacro debug [message & args]
  `(log/debug :line ~(:line (meta &form)) :message (pr-str ~message) ~@args))

(defmacro info [message & args]
  `(log/info :line ~(:line (meta &form)) :message (pr-str ~message) ~@args))

(defmacro warn [message & args]
  `(log/warn :line ~(:line (meta &form)) :message (pr-str ~message) ~@args))

(defmacro error [message & args]
  `(do
     ;; set *e to be error under current namespace.

     ;; NB given the form `(:error (Exception.))`, `(Exception.)` will be evaluated here
     ;; and also in the real `log/error` part.
     (when (= (get-env :app-dev nil) "true")
       (let [m# (hash-map ~@args)]
         (when-some [error# (:error m#)]
           ;; we could tap instead.
           (alter-var-root #'*e (constantly error#)))))
     (log/error :line ~(:line (meta &form)) :message (pr-str ~message) ~@args)))

(defmacro spy [expr]
  `(let [val# ~expr]
     (log/debug :line ~(:line (meta &form)) :message (pr-str "spy on form") :val val#)
     val#))

(comment
  (info (str "hi"))
  (error "hi" :error (ex-info "Exception" {}))
  (macroexpand-1 '(info (str "hi")))
  (macroexpand-1 '(error "hi" :error (ex-info "Exception" {}))))