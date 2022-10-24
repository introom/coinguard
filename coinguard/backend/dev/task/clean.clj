(ns task.clean
  (:require
   [babashka.fs :as fs]))

(defn rm [path]
  (when (fs/exists? path)
    (println "> delete file: " path)
    (fs/delete-tree path))
  nil)

(def ^:private clean-files
  ["target/"])

(defn clean
  []
  (doseq [file clean-files]
    (rm file)))