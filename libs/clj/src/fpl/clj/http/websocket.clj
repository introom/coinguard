(ns fpl.clj.http.websocket
  (:refer-clojure :exclude [send])
  (:require
   [fpl.clj.http.default :as d]
   [fpl.clj.namespace :refer [defalias]]
   [hato.websocket :as hw]))

;; see https://github.com/gnarroway/hato#websocket-options
(defn websocket
  "Builds a new WebSocket connection from a request object and returns a future connection.
  - `uri`: A websocket uri
  - `opts`:
    - `:http-client`: An HttpClient - will use a default HttpClient if not provided
    - `:listener`: A WebSocket$Listener - alternatively will be created from the handlers passed into opts:
                  :on-open, :on-message, :on-ping, :on-pong, :on-close, :on-error
    - `:headers` Adds the given name-value pair to the list of additional
                 HTTP headers sent during the opening handshake.
    - `:connect-timeout`: Timeout for establishing a WebSocket connection (default 5000ms).
    - `:subprotocols`: Sets a request for the given subprotocols."
  [uri opts]
  (let [opts (merge {:http-client d/default-client
                     :connect-timeout 5000}
                    opts)]
    (hw/websocket uri opts)))

(defalias send hw/send!)
(defalias close hw/close!)
(defalias ping hw/ping!)
(defalias pong hw/pong!)
(defalias abort hw/abort!)

(comment
  (let [ws @(websocket "ws://echo.websocket.events"
                       {:on-message (fn [ws msg last?]
                                      (println "Received message:" msg))
                        :on-close   (fn [ws status reason]
                                      (println "WebSocket closed!"))})]
    (send ws "Hello World!")
    (Thread/sleep 1000)
    (close ws)))