(ns app.db
  (:refer-clojure :exclude [update])
  (:require
   [app.logging :as log]
   [app.config :as config]
   [app.util.time :as time]
   [app.util.json :as json]
   [app.exception :as ex]
   [app.db.sql-impl :as sql]
   [buddy.core.hash :as hash]
   [integrant.core :as ig]
   [next.jdbc :as jdbc]
   [next.jdbc.date-time :as jdbc.date-time]
   [next.jdbc.result-set :as result-set])
  (:import
   (com.zaxxer.hikari HikariDataSource HikariConfig)
   (org.postgresql.util PGInterval PGobject)))

;; see https://www.postgresql.org/docs/11/runtime-config-client.html
(defn- init-sql []
  (str "SET statement_timeout = 300000;\n"
       "SET idle_in_transaction_session_timeout = 300000;"))

(defn- datasource-config [ctx]
  (let [url (:url ctx)
        username (:username ctx)
        password (:password ctx)
        config (HikariConfig.)]
    (doto config
      (.setJdbcUrl url)
      (.setUsername username)
      (.setPassword password)
      (.setPoolName "app-backend")
      (.setAutoCommit true)
      (.setReadOnly false)
      (.setConnectionTimeout 10000)  ;; 10s
      (.setValidationTimeout 10000)  ;; 10s
      (.setIdleTimeout 120000)       ;; 2min
      (.setMaxLifetime 1800000)      ;; 30min
      (.setMinimumIdle 3)
      (.setMaximumPoolSize 30)
      (.setConnectionInitSql (init-sql))
      (.setInitializationFailTimeout -1)
      #_stub)))

;; the first param is key
(defmethod ig/init-key ::db
  [_ _ctx]
  (log/info "start db")
  (let [ctx {:url (config/get :c/db-url)
             :username (config/get :c/db-username)
             :password (config/get :c/db-password)}
        ds (datasource-config ctx)]
    (jdbc.date-time/read-as-instant)
    (HikariDataSource. ds)))

(defmethod ig/halt-key! ::db
  [_ ds]
  (.close ^HikariDataSource ds))

(defn closed?
  [^HikariDataSource ds]
  (.isClosed ds))

(defn pgobject?
  ([v]
   (instance? PGobject v))
  ([v type]
   (and (instance? PGobject v)
        (= type (.getType ^PGobject v)))))

(defn pginterval [^String v]
  (PGInterval. v))

(defn decode-json-pgobject
  [^PGobject o]
  (let [typ (.getType o)
        val (.getValue o)]
    (if (or (= typ "json")
            (= typ "jsonb"))
      (json/read val)
      val)))

(defn json
  "Encode as plain json."
  [data]
  (doto (org.postgresql.util.PGobject.)
    (.setType "jsonb")
    (.setValue (json/write-str data))))

(defn interval ^PGInterval [v]
  (cond
    (integer? v)
    (->> (/ v 1000.0)
         ;; see https://www.postgresql.org/docs/current/datatype-datetime.html#DATATYPE-INTERVAL-INPUT
         (format "%s seconds")
         (pginterval))

    (string? v)
    (pginterval v)

    (time/duration? v)
    (->> (/ (.toMillis ^java.time.Duration v) 1000.0)
         (format "%s seconds")
         (pginterval))

    :else
    (ex/raise "Not implemented.")))

(defn execute-one
  ([ds sql]
   (execute-one ds sql {}))
  ([ds sql opts]
   (jdbc/execute-one! ds sql (merge
                              {:builder-fn result-set/as-unqualified-kebab-maps}
                              opts))))

(defn execute
  ([ds sql]
   (execute ds sql {}))
  ([ds sql opts]
   (jdbc/execute! ds sql (merge
                          {:builder-fn result-set/as-unqualified-kebab-maps}
                          opts))))

(defn query-one
  ([ds sql]
   (query-one ds sql {}))
  ([ds sql opts]
   (with-open [conn (jdbc/get-connection ds {:auto-commit false :read-only true})]
     (jdbc/execute-one! conn sql (merge
                                  {:builder-fn result-set/as-unqualified-kebab-maps}
                                  opts)))))

(defn query
  ([ds sql]
   (query ds sql {}))
  ([ds sql opts]
   (with-open [conn (jdbc/get-connection ds {:auto-commit false :read-only true})]
     (jdbc/execute! conn sql (merge
                              {:builder-fn result-set/as-unqualified-kebab-maps}
                              opts)))))

(defmacro with-transaction
  [& args]
  `(jdbc/with-transaction ~@args))

;; it is SAFE to truncate a digest to form a sub digest.
;; see https://crypto.stackexchange.com/a/9435
;; bigint is int64 in postgres
;; NB cannot make this function private because it is used in macro and get expanded elsewhere.
(defn hash-to-bigint*
  [^String s]
  (-> s
      hash/sha256
      (java.nio.ByteBuffer/wrap 0 8)
      (.getLong)))

(defmacro with-advisory-xact-lock
  "Must be called within a transaction."
  [^java.sql.Connection conn ^String lock-name & body]
  `(let [lock-id# (hash-to-bigint* ~lock-name)
         stmt# ["select pg_advisory_xact_lock(?)" lock-id#]]
     (jdbc/execute! ~conn stmt#)
     ;; the xact advisory lock is automatically released when the tx ends.
     ;; besides, we have no function to explictly release the xact lock
     ~@body))

;; common sql operations
(def ^:private default-opts {:return-keys true})

(defn insert
  ([ds table params]
   (insert ds table params nil))
  ([ds table params opts]
   (execute-one ds
                (sql/insert table params opts)
                (merge default-opts opts))))

(defn insert-multi
  ([ds table cols rows]
   (insert-multi ds table cols rows nil))
  ([ds table cols rows opts]
   (execute ds
            (sql/insert-multi table cols rows opts)
            (merge default-opts opts))))

(defn update
  ([ds table params where]
   (update ds table params where nil))
  ([ds table params where opts]
   (execute-one ds
                (sql/update table params where opts)
                (merge default-opts opts))))

(defn delete
  ([ds table params]
   (delete ds table params nil))
  ([ds table params opts]
   (execute-one ds
                (sql/delete table params opts)
                (merge default-opts opts))))

;; do not use the shipped `(jdbc.sql/get-by-id)` because we 
;; want to use our own `(execute)` functions with custom options.
(defn get-by-params
  ([ds table params]
   (get-by-params ds table params nil))
  ([ds table params opts]
   (execute-one ds (sql/select table params opts))))

(defn get-by-id
  ([ds table id]
   (get-by-params ds table {:id id} nil))
  ([ds table id opts]
   (get-by-params ds table {:id id} opts)))