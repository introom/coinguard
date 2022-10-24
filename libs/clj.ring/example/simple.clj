(ns simple
  (:require
   [ring.adapter.jetty9 :as jetty]))

(defn handler
  [req]
  (tap> req)
  {:status 200
   #_#_:headers {"content-type" "application/json"}
   :body (format "uri:%s" (:uri req))})

;; open http://localhost:8080
;; note that the browser will issue an extra reqeust to /favicon.ico
(defonce server (jetty/run-jetty #'handler {:port 8080 :join? false}))

(comment
  (.stop server)
  ;; bypass `defonce`
  (ns-unmap *ns* 'server))