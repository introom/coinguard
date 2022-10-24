(ns app.account
  (:require
   [app.db :as db]
   [app.exception :as ex]
   [buddy.hashers :as hashers]
   [honey.sql :as sql]
   [honey.sql.helpers :as h]))

;; see owasp for best pratices:
;; https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#password-storage-concepts
(defn- password->hash
  [password]
  (let [opts {:alg :argon2id}]
    ;; it already contains the salt.
    ;; see https://cljdoc.org/d/buddy/buddy-hashers/1.8.158/doc/user-guide#advanced-options
    (hashers/derive password opts)))

(defn- sql:create-account
  [username email password]
  (-> (h/insert-into :account)
      (h/values [{:username username
                  :email email
                  :password (password->hash password)}])
      (h/returning :*)
      sql/format))

(defn create-account
  [{:keys [db] :as ctx}
   {:keys [username email password] :as acct}]
  (db/execute-one db (sql:create-account username email password)))

(defn- sql:get-account
  [id email]
  (-> (h/select :*)
      (h/from :account)
      (as-> $
        (cond
          id (h/where $ [:= :id id])
          email (h/where $ [:= :email email])
          :else (ex/raise "Invalid session arguments"
                          :type ex/invalid
                          :code :invalid-arguments)))
      sql/format))

(defn get-account
  [{:keys [db] :as ctx}
   {:keys [id email] :as acct}]
  (db/query-one db (sql:get-account id email)))

(defn account-exists?
  ^Boolean [password hash]
  (:valid (hashers/verify password hash)))

(comment
  (-> (sql:create-account "firepanda"
                          "panda@firepanda.com"
                          "old panda")
      (sql/format {:inline true})))
