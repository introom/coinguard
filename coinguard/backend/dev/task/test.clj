(ns task.test
  (:refer-clojure :exclude [test])
  (:require
   [task.util :refer [clj]]))

(def ^:private test-directories
  ["test"])

(defn test
  []
  (let [cmd (format "-X:test :dirs '%s'"
                    (pr-str test-directories))]
    (clj cmd)))