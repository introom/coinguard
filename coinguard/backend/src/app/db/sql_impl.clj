(ns app.db.sql-impl
  "Provides common sql statements."
  (:refer-clojure :exclude [update])
  (:require
   [next.jdbc.sql.builder :as builder]
   [camel-snake-kebab.core :as csk]))

(def default-opts
  {:table-fn csk/->snake_case
   :column-fn csk/->snake_case})

(defn insert
  ([table key-map]
   (insert table key-map nil))
  ([table key-map opts]
   (let [opts (merge default-opts opts)
         opts (cond-> opts
                (:on-conflict-do-nothing opts)
                (assoc :suffix "ON CONFLICT DO NOTHING"))]
     (builder/for-insert table key-map opts))))

(defn insert-multi
  [table cols rows opts]
  (let [opts (merge default-opts opts)]
    (builder/for-insert-multi table cols rows opts)))

(defn select
  ([table where-params]
   (select table where-params nil))
  ([table where-params opts]
   (let [opts (merge default-opts opts)
         opts (cond-> opts
                (:for-update opts)    (assoc :suffix "FOR UPDATE")
                (:for-key-share opts) (assoc :suffix "FOR KEY SHARE"))]
     (builder/for-query table where-params opts))))

(defn update
  ([table key-map where-params]
   (update table key-map where-params nil))
  ([table key-map where-params opts]
   (let [opts (merge default-opts opts)]
     (builder/for-update table key-map where-params opts))))

(defn delete
  ([table where-params]
   (delete table where-params nil))
  ([table where-params opts]
   (let [opts (merge default-opts opts)]
     (builder/for-delete table where-params opts))))
