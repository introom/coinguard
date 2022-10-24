(ns fpl.clj.redis.util
  (:import
   [io.lettuce.core
    ScoredValue
    SetArgs]))

(defn map->scored-values
  [m]
  (->> m
       (map (fn [[k v]] (ScoredValue/just k v)))
       (into-array ScoredValue)))

(comment
  (map->scored-values {3.2 "hi"}))

(defn map->set-args
  [m]
  (cond-> (SetArgs.)
    (:ex m) (.ex (:ex m))
    (:px m) (.px (:px m))
    (:ex-at m) (.exAt (:ex-at m))
    (:px-at m) (.pxAt (:px-at m))
    (:nx m) .nx
    (:xx m) .xx
    (:keep-ttl m) .keepttl))

(comment
  (map->set-args {:ex 10 :nx true}))
