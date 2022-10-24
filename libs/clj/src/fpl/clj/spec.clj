;; XXX must read: see example model validation process:
;; https://gist.github.com/pithyless/0be222e1b1b3bca0239a9ca07d1b34c2

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; guideline
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; define a *separate* schema for
;; - gateway model
;; - db model
;; - domain models


;; for function instrumentation, refer to: 
;; https://github.com/metosin/malli/blob/master/docs/function-schemas.md#tldr
(ns fpl.clj.spec
  (:refer-clojure :exclude [assert])
  (:require
   [fpl.clj.process :refer [get-env]]
   [fpl.clj.exception :as ex]
   [fpl.clj.namespace :refer [defalias]]
   [malli.core :as m]
   [malli.transform :as mt]
   [malli.error :as me]))

(def ^:dynamic *compile-asserts* (get-env :fpl-clj-spec-compile-asserts true))

(defn -assert
  [spec x]
  (if (m/validate spec x)
    x
    (let [reason (-> (m/explain spec x)
                     me/humanize)]
      (ex/raise "Spec assertion failed." :code ::assertion-error :reason reason))))

(defmacro assert
  [spec x]
  (if *compile-asserts*
    `(-assert ~spec ~x)
    x))

(comment
  (assert :int 3)
  (assert :int "3"))

(defalias validate m/validate)
(defalias explain m/explain)
(defalias humanize me/humanize)

(defn coercer
  [schema options transformer]
  (let [schema (m/schema schema options)
        validator (m/validator schema options)
        decoder (m/decoder schema options transformer)
        explainer (m/explainer schema options)]
    (fn [x success failure]
      (let [ret (decoder x)]
        (if (validator ret)
          (success ret)
          (failure (explainer ret)))))))
