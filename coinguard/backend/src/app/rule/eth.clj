(ns app.rule.eth
  (:require
   [app.logging :as log]
   [app.chain.eth :refer [make-eth-request int->hex-str]]
   [app.wallet :as wallet]
   [app.rule.dispatch :refer [handle-task]]))

(defn- fetch-account-balance
  [node-url address height]
  (let [method "eth_getBalance"
        params [address (int->hex-str height)]]
    (->> (make-eth-request node-url method params))))

(defn- prepare-features
  [node-url address height]
  {:height height
   :balance (fetch-account-balance node-url address height)
   :previous-balance (fetch-account-balance node-url address (dec height))})

(defmethod handle-task :eth
  [{:keys [db chain]}
   {:keys [type address height] :as props}]
  (let [{:keys [node-url]} chain
        features (prepare-features node-url address height)
        wallets (wallet/get-wallets-by-address {:db db} address)]
    (doseq [{:keys [account-id data]} wallets]
      (let [{:keys [alerts]} data
            found (volatile! false)]
        (loop [[{:keys [conditions]} & as] alerts]
          ;; err only when all conditions have failed
          (loop [[{:keys [condition]} & cs] conditions]
            (println "asdf")
            ;; NB TODO
            (recur cs))

          (if @found
            (println "found")
            (when (seq as)
              (recur as))))))))

(defmulti execute-rule :type)

(defmethod execute-rule :amount
  [{{:keys [height balance previous-balance]} :features
    {:keys [address threshold]} :props}]
  (let [delta (- balance previous-balance)]
    (> delta threshold)))

(defmethod execute-rule :percent
  [{{:keys [height balance previous-balance]} :features
    {:keys [address threshold]} :props}]
  (let [deviation (/ (- balance previous-balance)
                     previous-balance)]
    (> deviation threshold)))

(comment
  (let [{:keys [a b c]} {:a 1, :b 2, :c 3}]
    [a b c])
  ;; see https://goerli.etherscan.io/
  (fetch-account-balance "https://goerli.infura.io/v3/9edaeabb8dd74a2a86b73a77f8c71232"
                         "0x769c0D9D3D282b85001E643F5E125de8a345e421"
                         6570493))


