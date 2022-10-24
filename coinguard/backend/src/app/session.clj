(ns app.session
  (:require
   [app.exception :as ex]
   [app.db :as db]
   [app.util.time :as time]
   [app.util.async :as async]
   [app.util.random :as random]
   [honey.sql.helpers :as h]
   [honey.sql :as sql]
   [integrant.core :as ig]
   [clojure.core.async :as a]))

(defn- sql:create-session
  [token account-id]
  (-> (h/insert-into :session)
      (h/values [{:token token :account-id account-id}])
      #_(s/on-conflict (s/do-nothing))
      (h/returning :*)
      sql/format))

(defn create-session
  [db account-id]
  (let [token (random/secure-string 16)]
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
                :else (ex/raise "invalid session arguments"
                                :type ex/invalid
                                :code :invalid-arguments)))
      sql/format))

(defn get-session
  [db {:keys [id token] :as session}]
  (db/query-one db (sql:get-session id token)))

;; session background daemon
(defn- sql:update-sessions
  [ids]
  (-> (h/update :session)
      (h/set {:updated_at :%now})
      (h/where [:= :id [:any (into-array String ids)]])
      (sql/format)))

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- async-update-sessions
  [db executor ids]
  (async/thread executor
                (db/execute-one db
                                (sql:update-sessions ids))
                (count ids)))

(def session-buffer-size 64)
(def batch-timeout-ms (* 30 1000))
(def batch-size 200)

;; session daemon service.  this daemon performs actions such as updating the last seen time.
(defmethod ig/init-key ::daemon
  [_ {:keys [db executor] :as ctx}]
  (let [event-ch (a/chan (a/dropping-buffer session-buffer-size))
        #_input #_(async/batch-input (:event-ch event-ch)
                                     {:batch-timeout-ms batch-timeout-ms
                                      :batch-size batch-size})]
    #_(a/go-loop []
        (when-let [{:keys [data reason]} (a/<! input)]
          (when-let [result (a/<! (async-update-sessions db executor data))]
            (recur))))
    (assoc ctx :event-ch event-ch)))

(defmethod ig/halt-key! ::daemon
  [_ ctx]
  (a/close! (:event-ch ctx)))

;; session gc
(def ^:private max-age
  "The time an inactive session can last."
  (time/duration {:days 3}))

(defn- sql:delete-expired
  [interval]
  (-> (h/delete-from :session)
      (h/where [:< :updated_at [:- :%now interval]])
      (sql/format)))

(defmethod ig/init-key ::gc-task
  [_ {:keys [db] :as opts}]
  (fn [_]
    (db/with-transaction [conn db]
      (let [interval (db/interval max-age)
            result   (db/execute-one conn (sql:delete-expired interval))
            result   (:next.jdbc/update-count result)]
        result))))

;; TODO ::events-ch is used to asynchronously update the session status
(comment
  (sql/format (-> {:select [:t.id [:name :item]], :from [[:table :t]], :where [:= :id 1]}
                  (h/where [:= :id 3]))))

(comment
  (require 'user)
  (def *db (:app.db/db user/system))
  (sql:create-session "asdf" "asdf")
  (create-session *db 333332))
