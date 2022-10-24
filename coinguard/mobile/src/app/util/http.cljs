(ns app.util.http
  (:refer-clojure :exclude [get])
  (:require
   ["axios$default" :as axios]
   [cljs.core.async :as a]
   [app.util.async :as aa]
   [app.util.data :as d]))

;; see https://axios-http.com/docs/req_config for the detailed configuration.
(defn- http-impl
  ([method url]
   (http-impl method url {}))
  ([method url config]
   (let [config (merge {:method (name method) :url url}
                       config)]
     (-> (.request axios (d/js config))
         (.then (fn [resp]
                  (d/clj resp)))))))

;; the methods all return a promise
(def get (partial http-impl :get))
(def post (partial http-impl :post))
(def put (partial http-impl :put))
(def patch (partial http-impl :patch))
(def delete (partial http-impl :delete))
(def head (partial http-impl :head))
(def options (partial http-impl :options))

(comment
  (a/go
    (def *a (aa/<p! (get "https://reqres.in/api/users/2"))))
  (a/go
    (try
      (def *a (aa/<p! (get "https://redqres.in/api/users/2")))
      (catch :default e
        (def *a e)
        (println e)))
    (println "execution done")))

