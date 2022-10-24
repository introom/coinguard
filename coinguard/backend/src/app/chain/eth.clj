(ns app.chain.eth
  (:require
   [app.util.retry :as retry]
   [app.logging :as log]
   [app.util.http :as http]))

(defn int->hex-str
  [n]
  (str "0x" (Long/toHexString n)))

(defn- make-eth-json-rpc-json
  [method params]
  {:jsonrpc '2.0 ':method method,
   :params params,
   :id (rand-int 1e5)})

(defn make-eth-request
  [node-url method params]
  (retry/retry {:max-retries 3}
               (-> (http/post node-url
                              {:form-params
                               (make-eth-json-rpc-json method params)})
                   (get-in [:body :result]))))