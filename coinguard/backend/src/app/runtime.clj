(ns app.runtime
  (:require
   [clojure.pprint :as pprint]
   [integrant.core :as ig]))

(prefer-method print-method
               clojure.lang.IRecord
               clojure.lang.IDeref)

(prefer-method pprint/simple-dispatch
               clojure.lang.IPersistentMap
               clojure.lang.IDeref)

(defmethod ig/prep-key :default
  [_ x]
  x)

(defmethod ig/init-key :default
  [_ x]
  x)
