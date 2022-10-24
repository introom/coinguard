(ns app.rule
  (:require
   [app.logging :as log]
   [app.rule.dispatch :refer [handle-task]]
   [app.rule.eth]))

(defn create-rule-task-handler
  [& {:keys [db] :as ctx}]
  (fn [{{:keys [type address block-height]} :props
        :as task}]
    (log/info "rule executing task invoked" :task task)
    (handle-task ctx task)))

(comment
  ;; (def *system (ig/init app.main/system-config [::rule-task-handler]))
  ;; (def *handler (::rule-task-handler *system))
  (def *handler (::rule-task-handler user/system))

  (*handler {:props {:type "eth",
                     :address "0x4c3ab77c956c05fb6efbba385ad8f67c7d6dcae0",
                     :block-height 6534873}})

  (*handler {:props {:type "sms"
                     :to-account "+19896970093"
                     :content "block alert"}}))