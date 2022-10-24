;; tutorial: https://juejin.cn/post/6844903954778701832
;; reference: https://lettuce.io/core/release/reference/index.html#overview
(ns fpl.clj.redis
  (:refer-clojure :exclude [set get type keys])
  (:require
   [clojure.string :as str]
   [fpl.clj.redis.util :as u])
  (:import
   [java.net URI]
   [java.time Duration]
   [io.lettuce.core RedisClient RedisURI]
   [io.lettuce.core.api StatefulRedisConnection]
   [io.lettuce.core.resource ClientResources DefaultClientResources]
   [io.lettuce.core.pubsub
    StatefulRedisPubSubConnection]))

;; see url format
;; https://github.com/lettuce-io/lettuce-core/wiki/Redis-URI-and-connection-details
(defn- param-map
  [^String url]
  (some-> url
          URI.
          .getRawQuery
          (str/split #"=|&")
          (->> (partition 2)
               (into {} (map vec)))))
(comment
  ;; gives {"timeout" "100ns"}
  (param-map "redis://localhost:16379/?timeout=100ns&foobar=false"))

(defn open-redis
  [{:keys [url timeout-ms]
    :or {timeout-ms 3000}}]
  (let [resources (.. (DefaultClientResources/builder)
                      (ioThreadPoolSize 4)
                      (computationThreadPoolSize 4)
                      build)
        params (param-map url)
        url (cond-> (RedisURI/create url)
              (contains? params "timeout") (.setTimeout (Duration/ofMillis timeout-ms)))

        client (proxy [RedisClient] [^ClientResources resources ^RedisURI url]
                 (close []
                   (.shutdown resources)
                   ;; close the client itself
                   (proxy-super close)))]
    (print (format "Redis connecting to address %s:%s" (.getHost url) (.getPort url)))
    client))

(defn close-redis
  [client]
  (.close client))

(defn open-connection
  [^RedisClient client]
  (.connect client))

(defn close-connection
  [^StatefulRedisConnection conn]
  (.close conn))

(defn open-pub-sub-connection
  [^RedisClient client]
  (.connectPubSub client))

(defn close-pub-sub-connection
  [^StatefulRedisPubSubConnection conn]
  (.close conn))

(defn with-connection
  ([conn f]
   (with-connection conn f {}))
  ([^StatefulRedisConnection conn f {:keys [async]}]
   (let [commands (if async (.async conn) (.sync conn))]
     (f commands))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; commands

;; hash
(defn hget
  [conn k f]
  (with-connection conn #(.hget % k f)))

(defn hgetall
  [conn k]
  (into {} (with-connection conn #(.hgetall % k))))

(defn hset
  [conn k m]
  (with-connection conn #(.hset % k m)))

(defn hdel
  [conn k & fs]
  (with-connection conn #(.hdel % k (into-array String fs))))

;; sorted set
(defn zadd
  "- `m`: A map of `{key:string value:double}`"
  [conn k m]
  (with-connection conn #(.zadd % k (u/map->scored-values m))))

(defn zrange
  [conn k zmin zmax]
  (with-connection conn #(.zrange % k zmin zmax)))

;; set
(defn sadd
  [conn k & vs]
  (with-connection conn #(.sadd % k (into-array String vs))))

(defn smembers
  [conn k]
  (with-connection conn #(.smembers % k)))

(defn scard
  [conn k]
  (with-connection conn #(.scard % k)))

(defn sismember
  [conn k v]
  (with-connection conn #(.sismember % k v)))

(defn srem
  [conn k & vs]
  (with-connection conn #(.srem % k (into-array String vs))))

;; connectivity
(defn ping
  [conn]
  (with-connection conn #(.ping %)))

;; basic ops
(defn set
  ([conn k v]
   (with-connection conn #(.set % k v)))
  ([conn k v {:keys [ex px ex-at px-at nx xx keep-ttl] :as opts}]
   (with-connection conn #(.set % k v (u/map->set-args opts)))))

(defn get
  [conn k]
  (with-connection conn #(.get % k)))

(defn mset
  [conn m]
  (with-connection conn #(.mset % m)))

(defn mget
  [conn & ks]
  (with-connection conn #(.mget % (into-array ks))))

(defn del
  [conn & ks]
  (with-connection conn #(.del % (into-array ks))))

(defn exists
  [conn & vs]
  (with-connection conn #(.exists % (into-array vs))))

(defn type
  [conn k]
  (with-connection conn #(.type % k)))

(defn keys
  [conn k]
  (with-connection conn #(.keys % k)))

(defn incr
  [conn k]
  (with-connection conn #(.incr % k)))

(defn incr-by
  [conn k v]
  (with-connection conn #(.incrby % k v)))

(defn decr
  [conn k]
  (with-connection conn #(.decr % k)))

(defn decr-by
  [conn k v]
  (with-connection conn #(.decrby % k v)))

;; lists
(defn lpush
  [conn k & vs]
  (with-connection conn #(.lpush % k (into-array String vs))))

(defn rpop
  ([conn k]
   (with-connection conn #(.rpop % k)))
  ([conn k cnt]
   (with-connection conn #(.rpop % k cnt))))

;; pub sub
(defn publish
  "Returns a `CompleteableFuture` when `async`.
   - `conn`: An pub-sub connection.
   - `chan`: A string denoting the topic channel.
   - `opts`:
     - `async`: Default to `true`."
  ([conn chan msg]
   (publish conn chan msg {}))
  ([conn chan msg {:keys [async] :as opts}]
   (with-connection conn #(.publish % chan msg) (merge {:async true} opts))))

(defn subscribe
  [conn & channels] 
  (with-connection conn #(.subscribe % (into-array String channels))))

(defn unsubscribe
  [conn & channels]
  (with-connection conn #(.unsubscribe % (into-array String channels))))

;; config
(defn config-set
  [conn & {:as m}]
  (with-connection conn #(.configSet % m)))
