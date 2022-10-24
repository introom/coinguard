#_(;; Allow this script to be executed directly
   "exec" "bb" --config "$(dirname $0)/../bb.edn" "$0" "$@"
   ;; add stub to disable calva moving the ending paren 
   stub)

;; this script is a sample of how to write a self-executable script
;; with bb.  for normal tasks, we go with `bb tasks`.

(ns ctl
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as str]
   [babashka.process :as process]
   [io.aviso.ansi :as ansi]))

;; we can read deps from deps.edn with
;; (babashka.deps/add-deps (clojure.edn/read-string (slurp "deps.edn")) {:aliases [:dev]})

(def ^:dynamic *silent* nil)
(def ^:dynamic *debug* nil)

(defn print-summary
  "Show summary"
  [summary commands]
  (println (str ansi/bold-green-font "Coinguard ctl" ansi/reset-font))
  (println)
  (println (str ansi/white-font "Usage:" ansi/reset-font))
  (println "<this-program> <global-options> <command> <command-options>")
  (println)
  (println (str ansi/white-font "Global options" ansi/reset-font))
  (println summary)
  (println)
  (println (str ansi/white-font "Commands" ansi/reset-font))
  (doseq [[command {:keys [description cli-options delegate]}] commands]
    (apply
     println
     (remove nil? [command (str ansi/yellow-font "(" description (when-not delegate " - coming soon!") ")" ansi/reset-font)]))
    (when-let [summary (:summary (parse-opts [] cli-options))]
      (when-not (str/blank? (str/trim summary))
        (println summary)))
    (println)))

;; see https://book.babashka.org/#_babashka_process
#_{:clj-kondo/ignore [:unused-binding]}
(defn migrate-db
  "Migrate database with flyway"
  [opts]
  (let [cmd ["clojure" "-X:dev" "tool.flyway/migrate"]
        proc (process/process cmd {:inherit true
                                   :shutdown process/destroy-tree})]
    ;; wait on proc
    ;; bb prints the value of the last form.  to discard it, we have to
    ;; (do (proc...) nil)
    @proc))

(def commands
  {"help"
   {:description (:doc (meta #'print-summary))
    :cli-options []
    ;; we explicitly call this special function `print-summary`, which needs the summary and commands arguments.
    :delegate nil}
   "migrate-db"
   {:description (:doc (meta #'migrate-db))
    :cli-options []
    :delegate migrate-db}})

(def global-cli-options
  [["-h" "--help" "Show this summary"]
   ["-s" "--silent" "Silent mode"]
   ["-D" "--debug" "Debug mode"]])

(defn -main []
  (let [{:keys [options arguments summary errors]}
        ;; `:in-order true` stops when we meet a non-option string
        (parse-opts *command-line-args*  global-cli-options :in-order true)

        command (first arguments)
        {:keys [cli-options delegate]} (commands (first arguments))]

    (cond
      (or (contains? options :help)
          (= command "help"))
      (print-summary summary commands)

      errors
      (doseq [err errors]
        (println err))

      (find commands command)
      (binding [*silent* (:silent options)
                *debug* (:debug options)]
        (if delegate
          (let [opts (parse-opts (next arguments) cli-options)]
            (when-let [err (some->> opts :errors (str/join ", "))]
              (println (str ansi/red-font "Error: " err ansi/reset-font)))
            (delegate
             (-> opts
                 (update :options merge options))))
          (println "No implementation:" command)))

      command
      (println "Unknown command:" command)

      :else
      (print-summary summary commands))))

(-main)