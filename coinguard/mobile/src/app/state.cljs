(ns app.state
  (:require
   [reagent.core :as r]
   [app.db :as db]
   [cljs.core.async :as a]
   [cljs.reader :refer [read-string]]
   [app.util.resource :refer [js-require-env fetch-local-file]]))

(defonce app-state
  (r/atom {:app/credential {}
           :wallet/list {}}))

(def persistent-states
  #{:app/credential})

(defn write-disk
  [kw]
  (db/write kw (kw @app-state)))

(defn- load-from-disk
  []
  (doseq [kw persistent-states]
    (when-some [data (db/read kw)]
      (swap! app-state assoc kw data))))

(defn- load-from-fixture
  [done-fn]
  ;; we have to force a recompile when the `APP_STATE_FIXTURE` value changes.
  (if-some [fixture (js-require-env "APP_STATE_FIXTURE")]
    (fetch-local-file fixture
                      (fn [content]
                        (let [m (read-string content)]
                          (swap! app-state merge m)
                          (done-fn))))
    (done-fn)))

(defn setup
  []
  (load-from-disk)
  (let [ch (a/chan)
        done-fn #(a/close! ch)]
    (if goog.DEBUG
      (load-from-fixture done-fn)
      (done-fn))
    ch))

(comment
  (setup)
  (swap! app-state assoc :app/credential {:username "foo" :password "bar"})
  (write-disk :app/credential))