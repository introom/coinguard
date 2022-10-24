(ns app.logging)

(defn- log*
  [level &form msg keyvals]
  `(log ~level
        ~(str *ns*)
        ~(:line (meta &form))
        ~msg
        ~keyvals))

(defmacro debug [msg & {:as keyvals}]
  (log* :debug &form msg keyvals))

(defmacro info [msg & {:as keyvals}]
  (log* :info &form msg keyvals))

(defmacro warn [msg & {:as keyvals}]
  (log* :warn &form msg keyvals))

(defmacro error [msg & {:keys [err]
                        :as keyvals}]
  `(do
     ~(log* :error &form msg keyvals)
     ;; add err to the current ns.
     ;; `~'x avoids x being qualified with the ns
     (when goog.DEBUG
       (set! ~'*e ~err))))
