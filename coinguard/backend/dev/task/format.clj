(ns task.format
  (:require
   [clojure.java.io :as io]
   [clojure.edn :refer [read-string]]
   [task.util :refer [clj]]))

(defn create-tempfile
  []
  (let [fname (format ".cljfmt-indentation%s.edn" (rand-int 1000))
        file (io/file fname)]
    (.deleteOnExit file)
    (spit file (pr-str (:indents (read-string (slurp ".cljfmt.edn")))))
    fname))

(defn fmt-base-cmd
  []
  (str "-Sdeps '{:deps {cljfmt/cljfmt {:mvn/version \"0.8.0\"}}}' -M -m cljfmt.main"
       ;; this space avoids sticking with the previous string
       " --indents " (create-tempfile)))

(def ^:private directories "dev/ src/ test/")

(defn check-fmt
  []
  (let [cmd (format "%s check %s" (fmt-base-cmd) directories)]
    (clj cmd)
    nil))

(defn fix-fmt
  []
  (let [cmd (format "%s fix %s" (fmt-base-cmd) directories)]
    (clj cmd)
    nil))