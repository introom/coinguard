(ns fpl.clj.http
  (:refer-clojure :exclude [get])
  (:require
   [fpl.clj.http.default :as d]
   [hato.client :as hc]))

(defn request
  [method url & [opts respond raise]]
  (hc/request (merge {:http-client d/default-client}
                     d/default-opts 
                     opts
                     {:request-method method :url url})
              respond
              raise))

(def get (partial request :get))
(def delete (partial request :delete))
(def head (partial request :head))
(def options (partial request :options))

;; see making queries:
;; https://github.com/gnarroway/hato#making-queries
;; both https://httpbin.org and https://reqres.in are good alternatives.
(comment
  (get "https://httpbin.org/get")
  (get "https://reqres.in/api/users?page=2")
  ;; or, via query-params
  (get "https://reqres.in/api/users" {:query-params {:page 2}})

  ;; headers must be string
  ;; see https://github.com/gnarroway/hato/blob/1a3790f058cce13173e4b10f1a1cd3556b09903c/src/hato/client.clj#L187
  (get "https://reqres.in/api/users" {:headers {"some-header" "some-value"}})

  ;; `oauth-token` middleware 
  ;; see https://github.com/gnarroway/hato/blob/ef829066afdaf458dd0299609e20162e65d32380/src/hato/middleware.clj#L426
  (get "https://reqres.in/api/users" {:oauth-token "my-secret"}))

(defn- with-form-params
  [method]
  (fn [url & [form-params opts respond raise]]
    (let [opts (merge opts {:form-params form-params})]
      (request method url opts respond raise))))

(def post (with-form-params :post))
(def put (with-form-params :put))
(def patch (with-form-params :patch))

(comment
  (post "https://reqres.in/api/users" {"name" "morpheus",
                                       "job" "leader"})
  ;; unsuccessful result (http 400)
  (post "https://reqres.in/api/login" {"email" "peter@klaven"}))
