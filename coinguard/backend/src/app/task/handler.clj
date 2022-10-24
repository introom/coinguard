(ns app.task.handler
  (:require
   [integrant.core :as ig]
   [app.alert :refer [create-mobile-task-handler]]
   [app.rule :refer [create-rule-task-handler]]))

(def rule-handler-name :rule-handler)
(def mobile-handler-name :mobile-handler)

(defmethod ig/init-key ::handler
  [_ {:keys [db] :as ctx}]
  {mobile-handler-name (create-mobile-task-handler :db db)
   rule-handler-name (create-rule-task-handler :db db)})