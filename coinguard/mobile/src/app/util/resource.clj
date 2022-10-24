(ns app.util.resource
  (:require
   [clojure.java.io :as io]))

;; NB this is the directory that contains index.js
(def ^:private base-dir "./target/app")

;; see https://reactnative.dev/docs/images
;; to quote, the image name in require has to be known statically.
;; the current path is target/app/index.js
(defmacro js-require
  [expr]
  (when-some [fname (some-> (eval expr)
                            ;; we can also use a function if as-> looks cluttered.
                            (as-> fname (format "../../resources/%s" fname))
                            (as-> fname (when (.isFile (io/file base-dir fname)) fname)))]
    `(js/require ~fname)))

(defmacro js-require-env
  [env]
  `(js-require (System/getenv ~env)))

(comment
  (macroexpand-1 '(js-require "i18n/en.json"))
  (macroexpand '(js-require-env "APP_STATE_FIXTURE")))
