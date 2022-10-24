(ns app.api.session
  (:require
   [app.account :as acct]
   [app.code :as code]
   [app.session :as session]
   [app.exception :as ex]
   [ring.util.http-response :as resp]))

(defn- create-session
  [{:keys [db] :as ctx}]
  (fn [req]
    (let [{:keys [email password]} (get-in req [:parameters :body])
          acct (acct/get-account {:db db} {:email email})]
      (if-not (acct/account-exists? password (:password acct))
        (ex/raise "Authentication failed"
                  :code code/authentication-failed)
        (resp/ok
         {:token (:token (session/create-session db (:id acct)))
          :account-id (:id acct)})))))

(def s:create-session
  [:map {:closed true}
   [:email string?]
   [:password string?]])

(defn router [ctx]
  ["/session"
   ["" {:post {:handler (create-session ctx)
               :parameters {:body s:create-session}}}]])

(comment
  (hash (find-ns 'app.session))
  (hash (get (ns-aliases 'app.api.session) 'session)))
