;; To avoid ambiguity, `topic` is redis channel, whereas `channel` is actually 
;; clojure async channel.
;; 
;; We maintain both the mapping of
;; - channel -> topics
;; - topic -> channels
;; inside the state variable.

(ns msgbus
  (:require
   [clojure.core.async :as a]
   [fpl.clj.async :as aa]
   [fpl.clj.concurrent :as cc]
   [fpl.clj.exception :as ex]
   [fpl.clj.redis :as rd])
  (:import
   io.lettuce.core.pubsub.RedisPubSubListener))

(declare start-io-loop)
(declare subscribe)
(declare purge)

(def ^:private xform-prefix-topic
  (map (fn [msg] (update msg :topic #(str "example-" %)))))

(defn start-msgbus
  [{:keys [buffer-size redis-url]
    :or {buffer-size 128}
    :as ctx}]
  (let [cmd-ch (a/chan buffer-size)
        rcv-ch (a/chan (a/dropping-buffer buffer-size))
        pub-ch (a/chan (a/dropping-buffer buffer-size) xform-prefix-topic)
        state  (agent {} :error-handler #(prn "Error occurred:" %))
        client (rd/open-redis {:url redis-url})
        control-fn (fn [& {:keys [cmd] :as params}]
                     (a/go
                       (case cmd
                         :pub   (a/>! pub-ch params)
                         :sub   (a/<! (subscribe ctx params))
                         :purge (a/<! (purge ctx params)))))
        ctx    (-> {:client client}
                   (assoc ::pub-conn (rd/open-connection client))
                   (assoc ::sub-conn (rd/open-pub-sub-connection client))
                   (assoc ::cmd-ch cmd-ch)
                   (assoc ::rcv-ch rcv-ch)
                   (assoc ::pub-ch pub-ch)
                   (assoc ::state state)
                   (assoc :control-fn control-fn))]
    (start-io-loop ctx)
    ctx))

(defn stop-msgbus
  [ctx]
  (a/close! (::cmd-ch ctx))
  (a/close! (::rcv-ch ctx))
  (rd/close-connection (::pub-conn ctx))
  (rd/close-pub-sub-connection (::sub-conn ctx))
  (rd/close-redis (:client ctx)))

(defn- conj-subscription
  [chans ctx topic ch]
  (let [chans (if (nil? chans) #{ch} (conj chans ch))]
    (when (= 1 (count chans))
      (rd/subscribe (::sub-conn ctx) topic))
    chans))

(defn- disj-subscription
  [chans ctx topic ch]
  (let [chans (disj chans ch)]
    (when (empty? chans)
      (rd/unsubscribe (::sub-conn ctx) topic))
    chans))

(defn- subscribe-to-topics
  [state ctx topics ch done-ch]
  (aa/with-close done-ch
    (let [state (update state ::channels assoc ch topics)]
      (reduce (fn [state topic]
                (update-in state [::topics topic] conj-subscription ctx topic ch))
              state
              topics))))

(defn- unsubscribe-single-channel
  [state ctx ch]
  (let [topics (get-in state [::channels ch])
        state  (update state ::channels dissoc ch)]
    (reduce (fn [state topic]
              (update-in state [::topics topic] disj-subscription ctx topic ch))
            state
            topics)))

(defn- unsubscribe-channels
  [state ctx channels done-ch]
  (aa/with-close done-ch
    (reduce #(unsubscribe-single-channel %1 ctx %2) state channels)))

(defn- subscribe
  [{:keys [::state executor] :as ctx} {:keys [topics ch]}]
  (let [done-ch (a/chan)]
    (send-via executor state subscribe-to-topics ctx topics ch done-ch)
    done-ch))

(defn- purge
  [{:keys [::state executor] :as ctx} {:keys [chans]}]
  (let [done-ch (a/chan)]
    (send-via executor state unsubscribe-channels ctx chans done-ch)
    done-ch))

(defn- create-listener
  [rcv-ch]
  (reify RedisPubSubListener
    (message [_ _pattern _topic _message])
    (message [_ topic message]
      ;; There are no back pressure, so we use a sliding
      ;; buffer for cases when the pub-sub broker sends
      ;; more messages than we can process.
      (let [val {:topic topic :message message}]
        (when-not (a/offer! rcv-ch val)
          (println "dropping message on subscription loop"))))
    (psubscribed [_ _pattern _count])
    (punsubscribed [_ _pattern _count])
    (subscribed [_ _topic _count])
    (unsubscribed [_ _topic _count])))

(defn publish-message
  [{:keys [::pub-conn]} {:keys [topic message]}]
  (let [done-ch (a/chan 1)]
    (-> (rd/publish pub-conn topic message)
        (cc/handle (fn [ret ex]
                     (when ex
                       (a/put! done-ch ex))
                     (prn ret)
                     (a/close! done-ch))))
    done-ch))

(defn start-io-loop
  [{:keys [::sub-conn ::rcv-ch ::pub-ch ::state executor] :as ctx}]

  (.addListener sub-conn (create-listener rcv-ch))

  (letfn [(fan-out-to-chans [topic message]
            (a/go-loop [chans  (seq (get-in @state [::topics topic]))
                        closed #{}]
              (if-let [ch (first chans)]
                (if (a/>! ch message)
                  (recur (rest chans) closed)
                  (recur (rest chans) (conj closed ch)))
                (seq closed))))

          (process-incoming [{:keys [topic message]}]
            (a/go
              (when-let [closed (a/<! (fan-out-to-chans topic message))]
                (send-via executor state unsubscribe-channels ctx closed nil))))]

    (a/go-loop []
      (let [[val port] (a/alts! [pub-ch rcv-ch])]
        (cond
          (nil? val)
          (do
            (println "stop io loop")
            (send-via executor state (fn [state]
                                       (->> (keys (::chans state))
                                            (filter some?)
                                            (run! a/close!))
                                       nil)))

          (= port rcv-ch)
          (do
            (a/<! (process-incoming val))
            (recur))

          (= port pub-ch)
          (let [result (a/<! (publish-message ctx val))]
            (when (ex/exception? result)
              (prn "Exception happened" result))
            (recur)))))))

(comment

  (get 3 a))
