(ns fpl.clj.git
  (:refer-clojure :exclude [hash])
  (:require
   [clojure.string :as str]
   [fpl.clj.process :refer [sh]]))

(defn sha []
  (str/trim (:out (sh "git rev-parse HEAD" {:out :string :verbose false}))))

(defn sha-short
  []
  (subs (sha) 0 7))

(comment
  (sha)
  (sha-short))
