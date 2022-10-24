;; we wrap `babashka.cli` instead of `clojure.tools.cli` because bb.cli
;; does not require defining a `cli-options` beforehand (as in tools.cli).

;; XXX see api: https://github.com/babashka/cli/blob/main/API.md
;; the cli layout is:
;; command sub-command ,,, options ,,, arguments
;; e.g., foo baz --flag on a b c 
(ns fpl.clj.cli
  (:require
   [clojure.string :as str]
   [babashka.cli :as cli]))

(def parse-opts cli/parse-opts)
(comment
  ;; {:paths "src"}
  (parse-opts ["--paths" "src" "--paths" "test"])
  ;; {:paths ["src" "test"] }
  (parse-opts ["--paths" "src" "--paths" "test"] {:coerce {:paths []}})
  ;; {:paths [:bar :baz]}
  (parse-opts ["--foo" "bar" "--foo" "baz"] {:coerce {:foo [:keyword]}})
  ;; {:verbose true}
  (parse-opts ["--verbose"])
  #_x)

(def parse-args cli/parse-args)
;; difference between `parse-opts` and `parse-args`:
(comment
  (parse-opts (str/split "foo foo2 --bar 1 baz" #" ")) ;; {:bar 1}
  (parse-args (str/split "foo foo2 --bar 1 baz" #" ")) ;; {:cmds ["foo" "foo2"], :opts {:bar 1} :args ["baz"]}
  nil)

;; spec
(def format-opts cli/format-opts)
(comment
  ;; see https://github.com/babashka/cli#spec
  ;; :from, :to are just random names.
  (def spec
    {:from   {:ref          "<src>"
              :desc         "The input format. <src> can be edn, json or transit."
              :coerce       :keyword
              :alias        :i
              :default-desc "edn"
              :default      :edn}
     :to     {:ref          "<dest>"
              :desc         "The output format. <dst> can be edn, json or transit."
              :coerce       :keyword
              :alias        :o
              :default-desc "json"
              :default      :json}})

  (println (format-opts {:spec spec :order [:to :from]})))

;; subcommand
;; see api: https://github.com/babashka/cli/blob/main/API.md#dispatch
(def dispatch cli/dispatch)

(comment
  (defn subcmd [m]
    (prn "invoked subcmd")
    (assoc m :fn :subcmd))

  (defn help [m]
    (prn "inovked help")
    (assoc m :fn :help))

  (def dispatch-table
    [{:cmds ["subcmd"] :cmds-opts [:file] :fn subcmd}
     {:cmds [] :fn help}])

  (defn -dispatch [& args]
    (dispatch dispatch-table args))
  
  (-dispatch "--help")
  (apply -dispatch (str/split "subcmd a --dry-run" #" "))
  (apply -dispatch (str/split "subcmd a b --dry-run" #" ")))
