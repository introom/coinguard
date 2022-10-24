;; see jsonista api:
;; https://cljdoc.org/d/metosin/jsonista/0.3.6/api/jsonista.core
(ns fpl.clj.json
  (:refer-clojure :exclude [read read-string])
  (:require
   [jsonista.core :as json]))

;; `jackson-databind` is used. see docs there:
;; https://github.com/FasterXML/jackson-databind
;; see also, https://cljdoc.org/d/metosin/jsonista/0.3.6/api/jsonista.core#object-mapper
(def ^:private default-mapper json/keyword-keys-object-mapper)

(defn mapper
  [params]
  (json/object-mapper params))

(defn write-bytes
  ([v] (json/write-value-as-bytes v default-mapper))
  ([v mapper] (json/write-value-as-bytes v mapper)))

(defn write-string
  ([v] (json/write-value-as-string v default-mapper))
  ([v mapper] (json/write-value-as-string v mapper)))

(defn- read
  ([v] (json/read-value v default-mapper))
  ([v mapper] (json/read-value v mapper)))

;; syntax sugar
(def read-string read)
(def read-bytes read)

(defn encode
  [v]
  (json/write-value-as-bytes v default-mapper))

(defn decode
  [v]
  (json/read-value v default-mapper))

(comment
  (write-string {:a :cd/b})

  ;; write java date object
  (write-string {:foo "bar" :baz (java.util.Date. 0)})

  (-> (write-string {:foo "bar" :baz (java.util.Date. 0)})
      read))
