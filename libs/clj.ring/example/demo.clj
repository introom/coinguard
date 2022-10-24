(ns demo
  (:require
   [integrant.core :as ig]
   [fpl.clj.logging :as log]
   [fpl.clj.system :as sys]
   [fpl.clj.ring :as rr]
   [fpl.ring.middleware :as middleware]
   [ring.util.http-status :as status]
   [reitit.ring :as ring]))

(def example-router
  (ring/router
   ["/" {:get (fn [req] {:status status/ok 
                         :body {:hi "go"}})}]
   {:data {:coercion rr/default-coercion
           :muuntaja rr/default-codec
           :middleware [middleware/format-middleware
                        middleware/parameters-middleware
                        middleware/coerce-exceptions-middleware
                        middleware/coerce-request-middleware
                        middleware/coerce-response-middleware]}}))

(defn- ring-handler [router]
  (let [handler (ring/ring-handler router
                                   (ring/routes
                                    #_(ring/create-resource-handler {:path "/"})
                                    (ring/create-default-handler)))]
    (fn [req]
      (log/info "received request" :req req)
      (try
        (handler req)
        (catch Throwable e
          (log/error "unhandled exception" :error e)
          {:status status/internal-server-error
           :body "Internal server error"})))))

(def handler (ring-handler example-router))

(defmethod ig/init-key ::server
  [_ {:as ctx}]
  (let [opts {:host "localhost" :port 8182}
        server (rr/start-server #'handler opts)]
    (assoc ctx :server server)))

(defmethod ig/halt-key! ::server
  [_ {:keys [server]}]
  (rr/stop-server server))

(def system-config
  {::server {}})

(defn init
  []
  (sys/setup system-config))

(init)

(comment
  (sys/start)
  (sys/stop))
