(ns app.api.account
  (:require
   [app.logging :as log]
   [app.code :as code]
   [app.account :as acct]
   [app.http.middleware :as middleware]
   [ring.util.http-status :as status]))

(def s:create-account
  [:map {:closed true}
   ;; XXX re does not have min/max properties
   [:email [:re #"^.+@.+\..+"]]
   [:password [:string {:min 8 :max 30}]]])

(defn- req->create-account [req]
  (let [{:keys [email password]} (get-in req [:parameters :body])]
    {:email email
     ;; extract foo from foo@bar.com
     :username (re-find #"^[^@]+" email)
     :password password}))

(defn- create-account
  [ctx]
  (fn [req]
    (->> (req->create-account req)
         (acct/create-account ctx))
    {:status status/ok
     :body {:code code/succeeded}}))

;; TODO throw unauthorized exception if the id of the current user does not match the 
;; id of the to be read user
(defn- get-account [ctx]
  (fn [req]))

(defn router [ctx]
  ["/accounts"
   ["" {:post {:handler (create-account ctx)
               :parameters {:body s:create-account}}}]
   ["/:id"] {:get {:handler (get-account ctx)}
             :middleware [middleware/session-auth]}])