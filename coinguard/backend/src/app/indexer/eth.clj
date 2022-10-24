(ns app.indexer.eth
  (:require
   [app.logging :as log]
   [integrant.core :as ig]
   [app.util.async :as aa]
   [app.db :as db]
   [app.chain.eth :refer [make-eth-request int->hex-str]]
   [clojure.core.async :as a]
   [app.task.handler :as handler]
   [app.config :as config]
   [app.task.worker :as worker]))

(def ^:private seen-height-range (atom []))

(defn- sync-block-height
  "Sync block height and return the new block range [start, end) else nil."
  [node-url height-range]
  (let [method "eth_blockNumber"
        params []]
    (-> (make-eth-request node-url method params)
        ;; this returns an Object intead of a primitive long.
        ;; compared to `parseLong`, this function can decode format like "0x32".
        Long/decode
        (as-> x
          (cond
            (empty? @height-range)
            (reset! height-range [x (inc x)])

            (> x (@height-range 1))
            (swap! height-range (fn [[_ end]] [end (inc x)])))))))

(defn- get-addresses
  [node-url height]
  (let [method "eth_getBlockByNumber"
        params [(int->hex-str height) true]]
    (->> (make-eth-request node-url method params)
         :transactions
         (transduce (mapcat (juxt :to :from)) conj #{}))))

(defn- parse-changed-addresses
  "Returns changed addresses in the block range [start, end) in the 
   format of:
   [{:height num
     :addresses [addr1, addr2]}
    {:height num+1
     :addresses [addr3, addr4, ...]}]"
  [node-url [start end]]
  (for [height (range start end)]
    {:height height
     :addresses (get-addresses node-url height)}))

(defn- submit-rule-tasks
  [db changed-addresses]
  (log/info (format "submit tasks.  %s blocks covered." (count changed-addresses)) )
  (doseq [{:keys [:height addresses]} changed-addresses
          address addresses]
    (worker/submit db
                   {:name handler/rule-handler-name
                    :props {:height height
                            :address address
                            :type :eth}
                    :queue :default
                    :priority 30
                    :schedule-delay 5000
                    :max-retries 3})))

(defn- refresh-changed-addresses
  "This function is meant to be called in a go block."
  [{:keys [node-url db]}]
  (db/with-transaction [conn db]
    (db/with-advisory-xact-lock conn "eth-indexer-lock"
      (log/info "refresh changed addresses")
      (some->>
       (sync-block-height node-url seen-height-range)
       (parse-changed-addresses node-url)
       (submit-rule-tasks conn)))))

(defmethod ig/init-key ::eth
  [_ {{:keys [close-mult phaser]} :ctrl
      :keys [db]
      :as ctx}]
  (log/info "eth indexer started")
  ;; start the loop
  (let [node-url (config/get :c/eth-node-url)
        poll-ms 3000
        close-ch (a/tap close-mult (a/chan))]
    (.register phaser)
    (a/go-loop []
      (let [timeout-ch (a/timeout poll-ms)
            [val port] (a/alts! [close-ch timeout-ch])]
        (condp = port
          close-ch
          (.arriveAndDeregister phaser)

          timeout-ch
          (do
            (a/<! (aa/thread ::aa/default
                             (refresh-changed-addresses {:node-url node-url
                                                         :db db})))
            (recur)))))))


(comment

  (log/info (format "hi"))

  (parse-changed-addresses
   "https://goerli.infura.io/v3/9edaeabb8dd74a2a86b73a77f8c71232"
   [5871215 5871219])
  
  (sync-block-height "https://goerli.infura.io/v3/9edaeabb8dd74a2a86b73a77f8c71232" 
                     (atom []))

  (make-eth-request "https://goerli.infura.io/v3/9edaeabb8dd74a2a86b73a77f8c71232"
                    "clique_getSignersAtHash"
                    ["0x8472965218489b2f9fd6de849b3a1d5848edcaa8cc74e45b107660c9e9491d7d"]))
