(ns fpl.ops.dev
  (:refer-clojure :exclude [test])
  (:require
   [fpl.clj.process :refer [get-env clj]]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [babashka.fs :as fs]))

(defn app-env
  []
  (get-env :app-env))

(defn rm [path]
  (when (fs/exists? path)
    (println "> delete file: " path)
    (fs/delete-tree path))
  nil)

(def ^:private default-clean-files
  ["target/"])

(defn clean
  [& paths]
  (let [paths (or paths default-clean-files)]
    (doseq [path paths]
      (rm path))))

(def ^:private antq-base-cmd
  ;; nice use of `pr-str`
  [:-Sdeps
   (pr-str {:deps {'antq/antq {:mvn/version "1.6.2"}
                   'org.slf4j/slf4j-nop {:mvn/version "2.0.0-alpha7"}}})
   :-M :-m :antq.core])

;; call the function with:
;; bb -m fpl.ops.dev/check-deps
(defn check-deps
  []
  (let [cmd antq-base-cmd]
    (try
      (clj cmd)
      (catch Exception _e))
    nil))

(comment
  (check-deps))

(defn upgrade-deps
  []
  ;; no `--force`.
  (let [cmd (concat antq-base-cmd [:--upgrade])]
    (clj cmd))
  nil)

(defn- create-temp-cljfmt-file
  "Slurps `.cljfmt.edn` file and put it under the `:indents` section."
  []
  (let [fname (format ".cljfmt-indentation%s.edn" (rand-int 1000))
        file (io/file fname)]
    (.deleteOnExit file)
    (spit file (pr-str (:indents (read-string (slurp ".cljfmt.edn")))))
    fname))

(defn format-base-cmd
  []
  (str "-Sdeps '{:deps {cljfmt/cljfmt {:mvn/version \"0.8.0\"}}}' -M -m cljfmt.main"
       (when (.exists (io/file ".cljfmt.edn"))
         (str " --indents " (create-temp-cljfmt-file)))))

(def ^:private default-format-directories ["dev/" "src/" "test/"])

(defn check-format
  [& dirs]
  (let [dirs (or dirs default-format-directories)
        cmd (format "%s check %s" (format-base-cmd) (str/join " " dirs))]
    (clj cmd)
    nil))

(defn fix-format
  [& dirs]
  (let [dirs (or dirs default-format-directories)
        cmd (format "%s fix %s" (format-base-cmd) (str/join " " dirs))]
    (clj cmd)
    nil))

(defn clj-kondo-base-cmd
  [dirs]
  (str "-Sdeps '{:deps {clj-kondo/clj-kondo  {:mvn/version \"2022.04.25\"}}}'"
       " -M -m clj-kondo.main"
       (when (.exists (io/file ".clj-kdon/config.edn"))
         " --config .clj-kondo/config.edn")
       " --lint " (str/join " " dirs)))

(defn lint
  [& dirs]
  (let [dirs (or dirs default-format-directories)
        cmd (clj-kondo-base-cmd dirs)]
    (clj cmd)))


(def ^:private default-test-directories
  ["test"])

;; example deps alias:
#_(:test
   {:extra-paths ["test"]
    :extra-deps
    {io.github.cognitect-labs/test-runner {:git/tag "v0.5.1"
                                           :git/sha "dfb30dd"}
     org.clojure/test.check {:mvn/version "1.1.1"}}
    :exec-fn cognitect.test-runner.api/test})

(defn clj-test
  [& dirs]
  (let [dirs (or dirs default-test-directories)
        cmd (format "-X:test cognitect.test-runner.api/test :dirs '%s'"
                    (pr-str dirs))]
    (clj cmd)))

;; deployment
;; see `kubectl set image --help` to update k8s image.
