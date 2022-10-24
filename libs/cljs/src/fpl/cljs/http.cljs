(ns fpl.cljs.http
  (:refer-clojure :exclude [get])
  (:require
   ["axios$default" :as axios]
   [cljs.core.async :as a]
   [fpl.cljs.async :as aa]
   [fpl.cljs.data :as d]))

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

;; the methods all return a promise.  modelled after axios api: 
;; https://axios-http.com/docs/api_intro#:~:text=Request%20method%20aliases
(def get (partial http-impl :get))
(def head (partial http-impl :head))
(def delete (partial http-impl :delete))
(def options (partial http-impl :options))

;; like in `axios`, we have a `data` parameter.
(defn- http-update-impl
  [method]
  (fn [url data config]
    (http-impl method url (merge config {:data data}))))
(def post (http-update-impl :post))
(def put (http-update-impl :put))
(def patch (http-update-impl :patch))

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


