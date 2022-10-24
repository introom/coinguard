(ns fpl.clj.namespace)

(defmacro -import-fn
  [name sym]
  (let [vr (resolve sym)
        m (meta vr)
        arglists (:arglists m)
        protocol (:protocol m)]
    (when (:macro m)
      (throw (IllegalArgumentException.
              (str "Calling import-fn on a macro: " sym))))
    `(do
       (def ~(with-meta name {:protocol protocol}) (deref ~vr))
       (alter-meta! (var ~name) merge (dissoc (meta ~vr) :name))
       ~vr)))

(defmacro -import-macro
  [name sym]
  (let [vr (resolve sym)
        m (meta vr)
        arglists (:arglists m)]
    (when-not (:macro m)
      (throw (IllegalArgumentException.
              (str "Calling import-macro on a non-macro: " sym))))
    `(do
       (def ~name ~(resolve sym))
       (alter-meta! (var ~name) merge (dissoc (meta ~vr) :name))
       (.setMacro (var ~name))
       ~vr)))

(defmacro -import-def
  [name sym]
  (let [vr (resolve sym)
        m (meta vr)
        name (with-meta name (if (:dynamic m) {:dynamic true} {}))
        nspace (:ns m)]
    `(do
       (def ~name @~vr)
       (alter-meta! (var ~name) merge (dissoc (meta ~vr) :name))
       ~vr)))

(defmacro defalias
  [name sym]
  (let [vr (resolve sym)
        m (meta vr)]
    (cond
      (nil? vr) `(throw (ex-info (format "`%s` does not exist" '~sym) {}))
      (:macro m) `(-import-macro ~name ~sym)
      (:arglists m) `(-import-fn ~name ~sym)
      :else `(-import-def ~name ~sym))))
