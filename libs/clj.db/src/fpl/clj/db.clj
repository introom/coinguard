(ns fpl.clj.db
  (:refer-clojure :exclude [update])
  (:require
   [fpl.clj.time :as time]
   [fpl.clj.json :as json]
   [fpl.clj.random :as random]
   [fpl.clj.exception :as ex]
   [fpl.clj.db.sql :as sql]
   [buddy.core.hash :as hash]
   [next.jdbc :as jdbc]
   [next.jdbc.date-time :as jdbc.date-time]
   [next.jdbc.result-set :as result-set]
   [next.jdbc.sql.builder :as builder])
  (:import
   (com.zaxxer.hikari HikariDataSource HikariConfig)
   (org.postgresql.util PGInterval PGobject)))

;; see https://www.postgresql.org/docs/14/runtime-config-client.html
(def ^:private init-sql
  (str "SET statement_timeout = 300000;\n"
       "SET idle_in_transaction_session_timeout = 300000;"))

(defn- create-datasource-config
  [{:keys [url username password auto-commit read-only config-fn]}]
  (let [config (HikariConfig.)]
    (doto config
      (.setJdbcUrl url)
      (.setUsername username)
      (.setPassword password)
      (.setPoolName (str "fpl-app-" (random/id 6)))
      (.setAutoCommit auto-commit)
      (.setReadOnly read-only)
      (.setConnectionTimeout 10000)  ;; 10s
      (.setValidationTimeout 10000)  ;; 10s
      (.setIdleTimeout 120000)       ;; 2min
      (.setMaxLifetime 1800000)      ;; 30min
      (.setMinimumIdle 3)
      (.setMaximumPoolSize 30)
      (.setConnectionInitSql init-sql)
      (.setInitializationFailTimeout -1)
      config-fn)))

(defn open
  "- `read-only`: Defaults to `false`.
   - `auto-commit`: Defaults to `true`.
   - `config-fn`: Defaults to `identity`. 
      The function is used to update the datasource configuration."
  [{:keys [url username password read-only auto-commit config-fn]
    :as opts}]
  (jdbc.date-time/read-as-instant)
  (let [opts (merge {:read-only false :auto-commit true :config-fn identity}
                    opts)
        ds (create-datasource-config opts)]
    (HikariDataSource. ds)))

(defn close
  [^HikariDataSource ds]
  (.close ds))

(defn closed?
  [^HikariDataSource ds]
  (.isClosed ds))

(defn pgobject?
  ([v]
   (instance? PGobject v))
  ([v type]
   (and (instance? PGobject v)
        (= type (.getType ^PGobject v)))))

(defn decode-json
  [^PGobject o]
  (let [typ (.getType o)
        data (.getValue o)]
    (case typ
      "json" (json/read-string data)
      "jsonb" (json/read-bytes data)
      data)))

(defn encode-json
  "Encode as plain json."
  [data]
  (doto (org.postgresql.util.PGobject.)
    (.setType "jsonb")
    (.setValue (json/write-string data))))

(defn- interval* [^String v]
  (PGInterval. v))

(defn interval ^PGInterval [v]
  (cond
    (string? v) (interval* v)

    (integer? v)
    (->> (time/ms->sec v)
         ;; see https://www.postgresql.org/docs/current/datatype-datetime.html#DATATYPE-INTERVAL-INPUT
         (format "%s seconds")
         interval*)

    (instance? java.time.Duration v)
    (->> (time/duration->sec v)
         (format "%s seconds")
         interval*)

    :else
    (ex/raise "Not implemented.")))

(def ^:private default-execution-opts
  {:return-keys true
   ;; qualified keywords so no surprise when we merge two tables.
   :builder-fn result-set/as-kebab-maps})

(defn execute-one
  ([ds sql]
   (execute-one ds sql {}))
  ([ds sql opts]
   (jdbc/execute-one! ds sql (merge default-execution-opts opts))))

(defn execute-many
  ([ds sql]
   (execute-many ds sql {}))
  ([ds sql opts]
   (jdbc/execute! ds sql (merge default-execution-opts opts))))

;; example usage on `jdbc/get-connection`.
#_(with-open [conn (jdbc/get-connection ds {:auto-commit false :read-only true})]
    (execute-many conn  (merge default-execution-opts opts)))

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
      .getLong))

(defmacro with-advisory-xact-lock
  "Must be called within a transaction."
  [^java.sql.Connection conn ^String lock-name & body]
  `(let [lock-id# (hash-to-bigint* ~lock-name)
         stmt# ["select pg_advisory_xact_lock(?)" lock-id#]]
     (jdbc/execute! ~conn stmt#)
     ;; the xact advisory lock is automatically released when the tx ends.
     ;; besides, we have no function to explictly release the xact lock
     ~@body))

;; see docs
;; https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.2.790/doc/getting-started/friendly-sql-functions
;; and the relevant source code.
(defn insert
  ([ds table params]
   (insert ds table params nil))
  ([ds table params opts]
   (execute-one ds (sql/insert table params opts))))

(defn insert-many
  ([ds table cols rows]
   (insert-many ds table cols rows nil))
  ([ds table cols rows opts]
   (execute-many ds (sql/insert-many table cols rows opts) opts)))

(defn update
  ([ds table params where]
   (update ds table params where nil))
  ([ds table params where opts]
   (execute-one ds (sql/update table params where opts) opts)))

(defn delete
  ([ds table params]
   (delete ds table params nil))
  ([ds table params opts]
   (execute-one ds (sql/delete table params opts) opts)))

(defn select
  ([ds table params]
   (select ds table params nil))
  ([ds table m opts]
   (execute-one ds (sql/select table m opts) opts)))

(defn select-many
  ([ds table params]
   (select-many ds table params nil))
  ([ds table m opts]
   (execute-many ds (sql/select table m opts) opts)))

(defn get-by-id
  ([ds table id]
   (get-by-id ds table {:id id} nil))
  ([ds table id opts]
   (execute-one ds (sql/select table {:id id} opts) opts)))

(comment
  #_{:clj-kondo/ignore true}
  (next.jdbc.sql.builder/for-insert))
