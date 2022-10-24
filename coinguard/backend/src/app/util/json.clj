(ns app.util.json
  (:refer-clojure :exclude [read])
  (:require
   [jsonista.core :as j]))

(defn mapper
  [params]
  (j/object-mapper params))

(defn write
  ([v] (j/write-value-as-bytes v j/keyword-keys-object-mapper))
  ([v mapper] (j/write-value-as-bytes v mapper)))

(defn write-str
  ([v] (j/write-value-as-string v j/keyword-keys-object-mapper))
  ([v mapper] (j/write-value-as-string v mapper)))

(defn read
  ([v] (j/read-value v j/keyword-keys-object-mapper))
  ([v mapper] (j/read-value v mapper)))

(defn encode
  [v]
  (j/write-value-as-bytes v j/keyword-keys-object-mapper))

(defn decode
  [v]
  (j/read-value v j/keyword-keys-object-mapper))

(comment
  (write-str {:a :cd/b})
  (= (write-str {:a :b}) (write-str {:a "b"})))