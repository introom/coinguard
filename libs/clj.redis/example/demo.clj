(ns demo
  (:require
   [fpl.clj.redis :as rd]))

(def client (rd/open-redis {:url "redis://localhost:16379"}))
(def conn (rd/open-connection client))

(comment
  (rd/ping conn)

  (rd/hset conn "asdf" {"hello" "world"})
  (rd/hget conn "asdf" "hello")
  (rd/hgetall conn "asdf")
  (rd/hdel conn "asdf" "hello")

  (rd/zadd conn "set" {100 "happy"})
  (rd/zrange conn "set" 1 4)

  (rd/sadd conn "myset" "a" "b")
  (rd/smembers conn "myset")
  (rd/sismember conn "myset" "a")
  (rd/srem conn "myset" "a")

  (rd/set conn "a-key" "some-val" {:ex 3})
  (rd/get conn "a-key")
  (rd/exists conn "a-key")

  (rd/lpush conn "queue" "fasdf")
  (rd/rpop conn "queue" 1)

  (rd/config-set conn "notify-keyspace-events" "Ex"))

  ;stub)

;; pub sub
(def pub-sub-conn (rd/open-pub-sub-connection client))

(comment
  ;; the returned value is an instance of j.c.u.Future
  @(rd/publish pub-sub-conn "topic" "hello")
  (rd/subscribe pub-sub-conn "topic")
  (rd/unsubscribe pub-sub-conn "topic")

  )
