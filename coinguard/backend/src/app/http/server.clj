(ns app.http.server
  (:require
   [integrant.core :as ig]
   ;; see https://ring-clojure.github.io/ring/ring.adapter.jetty.html
   [ring.adapter.jetty :as jetty]
   [app.config :as config]
   [app.http.router :refer [create-router]]
   [reitit.ring :as ring]
   [app.logging :as log]
   [ring.util.http-status :as status]))

(defn- router-handler [router]
  (let [handler (ring/ring-handler router
                                   (ring/routes
                                    #_(ring/create-resource-handler {:path "/"})
                                    (ring/create-default-handler)))]
    ;; there is middleware support on exception: 
    ;; https://cljdoc.org/d/metosin/reitit/0.5.15/doc/ring/exception-handling-with-ring
    ;; but no bother for now.
    (fn [req]
      (log/debug "received request" :req req)
      (try
        (handler req)
        (catch Throwable e
          (log/error "unhandled exception" :error e)
          {:status status/internal-server-error
           :body "Internal server error"})))))

(defmethod ig/init-key ::server
  [_ {:keys [db session] :as ctx}]
  (let [host (config/get :c/http-host)
        port (config/get :c/http-port)
        address (format "%s:%s" host port)
        opts {:host host
              :port port
              :send-server-version? false
              :join? false}
        router (create-router :db db :session session)
        server (jetty/run-jetty (router-handler router) opts)]

    (log/info "starting portal http server" :address address)
    (assoc ctx :server server :address address :router router)))

(defmethod ig/halt-key! ::server
  [_ {:keys [server address]}]
  (log/info "stopping portal http server"
            :address address)
  ;; NB wait since stop is async.
  (doto server .stop .join))
