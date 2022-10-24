(ns fpl.ops.kubernetes
  (:require
   [clojure.string :as str]))

(defn helm-set
  "Takes a vector of `[[attr val], [attr2 val2],,,]` and
   returns its string concatenation."
  [attrs]
  (->> attrs
       (map (fn [[a v]]
              (format "--set %s=%s" a v)))
       (str/join " ")))

(comment
  (helm-set [])
  (helm-set [["foo" "bar"] ["baz" "fox"]]))
