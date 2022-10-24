(ns app.util.http
  (:refer-clojure :exclude [get])
  (:require
   [hato.client :as hc]))

;; see https://github.com/gnarroway/hato#building-a-client
;; sensible defaults
(def ^:private default-client (hc/build-http-client {:connect-timeout 10000
                                                     :redirect-policy :always}))

(def ^:private default-opts {;; request
                             :content-type :json
                             ;; response
                             :as :json :coerce :always})

(defn- configure-and-execute
  "Convenience wrapper"
  [method url & [opts respond raise]]
  (hc/request (merge {:http-client default-client}
                     default-opts
                     opts
                     {:request-method method :url url})
              respond
              raise))

(def get (partial configure-and-execute :get))
(def post (partial configure-and-execute :post))
(def put (partial configure-and-execute :put))
(def patch (partial configure-and-execute :patch))
(def delete (partial configure-and-execute :delete))
(def head (partial configure-and-execute :head))
(def options (partial configure-and-execute :options))