(ns fpl.clj.db.sql
  "Provides common sql statements."
  (:refer-clojure :exclude [update])
  (:require
   [next.jdbc.sql.builder :as builder]
   [camel-snake-kebab.core :as csk]))

(def default-sql-opts
  {:table-fn csk/->snake_case
   :column-fn csk/->snake_case})

;; if unsure, see the `builder` source for the accepted `opts` argument.
(defn insert
  [table key-map opts]
  (let [opts (merge default-sql-opts opts)
        opts (cond-> opts
               (:on-conflict-do-nothing opts)
               (assoc :suffix "ON CONFLICT DO NOTHING"))]
    (builder/for-insert table key-map opts)))

(comment
  (insert :foo {:a 3} nil))

(defn insert-many
  [table cols rows opts]
  (let [opts (merge default-sql-opts opts)]
    (builder/for-insert-multi table cols rows opts)))

(defn select
  [table where-params opts]
  (let [opts (merge default-sql-opts opts)
        opts (cond-> opts
               (:for-update opts)    (assoc :suffix "FOR UPDATE")
               (:for-key-share opts) (assoc :suffix "FOR KEY SHARE"))]
    (builder/for-query table where-params opts)))

(defn update
  [table key-map where-params opts]
  (let [opts (merge default-sql-opts opts)]
    (builder/for-update table key-map where-params opts)))

(defn delete
  [table where-params opts]
  (let [opts (merge default-sql-opts opts)]
    (builder/for-delete table where-params opts)))