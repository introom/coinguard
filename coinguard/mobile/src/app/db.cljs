(ns app.db
  (:require
   [cljs.reader :as reader]
   ["react-native-mmkv" :refer [MMKV]]))

(defonce storage (MMKV.))

(defn- qualified-name
  [kw-sym]
  (-> kw-sym symbol str))

(defn write
  [key val]
  (.set storage (qualified-name key) (pr-str val)))

(defn read
  ([kw-sym]
   (read kw-sym nil))
  ([kw-sym default]
   (if-some [data (.getString storage (qualified-name kw-sym))]
     (reader/read-string data)
     default)))

(comment
  (.getAllKeys storage)

  (read :app/credential)

  (.clearAll storage)

  (write :adsf/da {:a 3 :b "4"})
  (read :adsf/da)

  (write 'kk {:a :b})
  (read 'kk))
