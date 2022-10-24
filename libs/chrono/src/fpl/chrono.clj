(ns fpl.chrono
  (:require
   [fpl.clj.namespace :refer [defalias]]
   [fpl.chrono.worker :as worker]))

(defalias start-worker worker/start-worker)

(defalias stop-worker worker/stop-worker)

(defalias submit-task worker/submit-task)