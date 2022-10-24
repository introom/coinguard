(ns app.api.wallet
  (:require
   [app.wallet :as wallet]
   [app.code :as code]
   [app.logging :as log]
   [malli.util :as mu]
   [app.schema.wallet :as sw]
   [ring.util.http-status :as status]))

(def s:create-wallet
  (-> (mu/dissoc sw/s:wallet :id)
      #_(mu/update :data #(mu/update-properties % assoc :optional true))))

(def s:update-wallet
  sw/s:wallet)

(def s:get-wallets-by-account
  [:map {:closed true}
   [:account-id uuid?]])

(def s:wallet-id
  [:map {:closed true}
   [:id uuid?]])

(defn- create-wallet
  [{:keys [db] :as ctx}]
  (fn [req]
    (let [{:keys [account-id data] :as w} (get-in req [:parameters :body])]
      (wallet/create-wallet ctx w)
      {:status status/ok
       :body {:code code/succeeded}})))

(defn- get-wallets
  [{:keys [db] :as ctx}]
  (fn [req]
    (let [{:keys [account-id]} (get-in req [:parameters :query])
          ws (wallet/get-wallets-by-account ctx account-id)]
      {:status status/ok
       :body ws})))

(defn- update-wallet
  [{:keys [db] :as ctx}]
  (fn [req]
    (let [{:keys [id]} (get-in req [:parameters :path])
          data (get-in req [:parameters :body])]
      (wallet/update-wallet ctx id data)
      {:status status/ok
       :body {:code code/succeeded}})))

(defn- delete-wallet
  [{:keys [db] :as ctx}]
  (fn [req]
    (let [{:keys [id]} (get-in req [:parameters :path])]
      (wallet/delete-wallet ctx id)
      {:status status/ok
       :body {:code code/succeeded}})))

(defn router [ctx]
  ["/wallets"
   ["" {:post {:handler (create-wallet ctx)
               :parameters {:body s:create-wallet}}
        ;; TODO get request with authentication
        :get {:handler (get-wallets ctx)
              :parameters {:query s:get-wallets-by-account}}}]
   ["/:id" {:put {:handler (update-wallet ctx)
                  :parameters {:body sw/s:data
                               :path s:wallet-id}}
            :delete {:handler (delete-wallet ctx)
                     :parameters {:path s:wallet-id}}}]])

(comment
  ((requiring-resolve 'malli.generator/generate)
   s:create-wallet)

  ((requiring-resolve 'malli.generator/generate) 
   s:update-wallet)

  ((requiring-resolve 'malli.generator/generate) 
   pos-int? {:seed 100, :size 5}))
