(ns app.rule.dispatch)

(defmulti handle-task
  (fn [ctx task]
    (get-in task [:props :type])))