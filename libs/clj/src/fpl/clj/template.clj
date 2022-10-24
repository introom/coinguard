;; see https://github.com/yogthos/Selmer#usage

;; some useful filters are:
;; https://github.com/yogthos/Selmer/blob/master/README.md#empty
;; empty?, not-empty
(ns fpl.clj.template
  "Wrapper around `selmer`."
  (:require
   [selmer.parser :as parser]
   [selmer.filters :as filters]))

(def render parser/render)
;; NB cannot use `f.n.import-vars` because `bb` does not support `.setMacro` method.
(defmacro << [& xs] `(parser/<< ~@xs))

(comment
  (render "Hello {{name}}!" {:name "Foo"})
  (let [name "Foo"]
    (<< "Hello {{name| upper}}")))

(def add-filter! filters/add-filter!)

(defn render-path
  "Renders a template (e.g., k8s manifest) given a local `path` and a context map `m`.
   Also accepts an `opts` map. See https://github.com/yogthos/Selmer#custom-markers"
  [path m & [opts]]
  (let [tmpl (slurp path)
        output (render tmpl m opts)]
    output))

;; render-file takes the file path on the classpath.
(def render-file parser/render-file)