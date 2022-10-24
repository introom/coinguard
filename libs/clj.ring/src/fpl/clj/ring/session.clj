(ns fpl.clj.ring.session
  (:require
   [fpl.clj.process :refer [get-env]]
   [fpl.clj.exception :as ex]
   [fpl.clj.async :as aa]
   [fpl.db :as db]
   [fpl.clj.random :as random]
   [fpl.clj.spec :as s]
   [honey.sql.helpers :as h]
   [honey.sql :as sql]
   [clojure.core.async :as a]))

(defn- sql:create-session
  [token account-id]
  (-> (h/insert-into :session)
      (h/values [{:token token :account-id account-id}])
      #_(s/on-conflict (s/do-nothing))
      (h/returning :*)
      sql/format))

(comment
  (sql:create-session "1779" "john"))

(defn create-session
  [db account-id]
  (let [token (random/id 16)]
    (db/execute-one db (sql:create-session token account-id))))

(defn- sql:delete-session
  [id]
  (-> (h/delete-from :session)
      (h/where [:= :id id])
      sql/format))

(defn delete-session
  [db id]
  (db/execute-one db (sql:delete-session id)))

(defn- sql:get-session
  [id token]
  (-> (h/select :*)
      (h/from :session)
      (as-> $ (cond
                id (h/where $ [:= :id id])
                token (h/where $ [:= :token token])
                :else (ex/raise "Invalid session arguments"
                                :code :session/invalid-argument)))
      sql/format))

(defn get-session
  [db {:keys [id token] :as session}]
  (db/execute-one db (sql:get-session id token)))

;; session background daemon
(defn- sql:update-sessions
  [ids]
  (-> (h/update :session)
      (h/set {:updated_at :%now})
      (h/where [:= :id [:any (into-array String ids)]])
      (sql/format)))

(defn- async-update-sessions
  [db executor ids]
  (aa/thread executor
             (db/execute-one db
                             (sql:update-sessions ids))
             (count ids)))

(def session-buffer-size (get-env :fpl-session-buffer-size 64))
(def batch-timeout-ms (get-env :fpl-session-batch-timeout-ms (* 30 1000)))
(def batch-size (get-env :fpl-session-batch-size 200))

;; session daemon service.  this daemon performs actions such as updating the last seen time.
(defn start-daemon
  [{:keys [db executor]}]
  (let [session-ch (a/chan (a/dropping-buffer session-buffer-size))
        batch-ch (aa/batch-ch session-ch
                              {:batch-timeout-ms batch-timeout-ms
                               :batch-size batch-size})]
    (a/go-loop []
      (when-let [{:keys [data status]} (a/<! batch-ch)]
        (s/assert [:sequential int?] data)
        (a/<! (async-update-sessions db executor data))
        (recur)))

    session-ch))

(defn stop-daemon
  [session-ch]
  (a/close! session-ch))