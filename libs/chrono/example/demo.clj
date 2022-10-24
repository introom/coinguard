(ns demo
  (:require
   [fpl.clj.db :as db]
   [fpl.clj.process :refer [get-env]]
   [fpl.clj.concurrent :as con]
   [fpl.chrono :as chrono]))

(def db-url (get-env :app-db-url "jdbc:postgresql://localhost:15432/matthew"))
(def db-username (get-env :app-db-username "matthew"))
(def db-password (get-env :app-db-password "matthew"))

(def db (db/open {:url db-url :username db-username :password db-password}))
(def executor (con/thread-pool-executor {:core-pool-size 1}))

(comment
  (chrono/submit-task db {:name "mobile"
                          :props {:a :b}
                          :queue "default"
                          :priority 30
                          :poll-ms 5000
                          :max-retries 4}))

(comment
  (def *w (chrono/start-worker {:db db :executor executor}))
  (chrono/stop-worker *w))
