(ns app.wallet
  (:require
   [app.logging :as log]
   [app.db :as db]
   [app.exception :as ex]
   [honey.sql.helpers :as h]
   [honey.sql :as sql]))

(defn- sql:create-wallet
  [account-id data]
  (-> (h/insert-into :wallet)
      (h/values [{:account-id account-id
                  :data (db/json data)}])
      (h/returning :*)
      sql/format))

(defn- sql:update-wallet
  [id data]
  (-> (h/update :wallet)
      (h/set {:data (db/json data)})
      (h/where [:= :id id])
      sql/format))

(defn- sql:delete-wallet
  [id]
  (-> (h/delete-from :wallet)
      (h/where [:= :id id])
      sql/format))

(defn create-wallet
  [{:keys [db] :as ctx}
   {:keys [account-id data] :as w}]
  (db/execute-one db (sql:create-wallet account-id data)))

(defn- sql:get-wallets-by-account
  [account-id]
  (-> (h/select :*)
      (h/from :wallet)
      (h/where [:= :account-id account-id])
      sql/format))

(defn get-wallets-by-account
  "Returns a sequence of wallets in the format of 
   [{:id uuid
     :account-id uuid
     :data {:address foo 
            :coin bar
            ,,,}}
    ,,,]"
  [{:keys [db] :as ctx} account-id]
  ;; decode json pg objects
  (->> (db/query db (sql:get-wallets-by-account account-id))
       (mapv #(update % :data db/decode-json-pgobject))))

(defn- sql:get-wallets-by-address
  [address]
  (-> (h/select :*)
      (h/from :wallet)
      (h/where [:= [:raw "(data->>'address')"] address])
      sql/format))

(defn get-wallets-by-address
  [{:keys [db] :as ctx}
   address]
  (->> (db/query db (sql:get-wallets-by-address address))
       (mapv #(update % :data db/decode-json-pgobject))))

(defn update-wallet
  [{:keys [db] :as ctx}
   id data]
  (db/execute-one db (sql:update-wallet id data)))

(defn delete-wallet
  [{:keys [db] :as ctx}
   id]
  (let [ret (db/execute-one db (sql:delete-wallet id))]
    (when (zero? (:next.jdbc/update-count ret))
      (ex/raise "deletion failed"
                :type ex/invalid
                :code :invalid-arguments))))

(comment
  (def *db (:app.db/db user/system))
  (get-wallets-by-address {:db *db} "RqggYA1T8MJBz2"))