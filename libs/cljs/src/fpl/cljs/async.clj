(ns fpl.cljs.async)

;; (potemkin/import-vars 
;;  [interop <p!])

;; like `cljs.core.async.interop/<p!` but we unwraps the error.
(defmacro <p!
  [expr]
  `(let [v# (cljs.core.async/<! (cljs.core.async.interop/p->c ~expr))]
     (if (and (instance? cljs.core/ExceptionInfo v#)
              (= (:error (ex-data v#)) :promise-error))
       (throw (ex-cause v#))
       v#)))
