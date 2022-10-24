(ns fpl.cljs.data
  (:refer-clojure :exclude [js-keys])
  (:require
   ;; docs: https://clj-commons.org/camel-snake-kebab/
   [camel-snake-kebab.core :as csk]
   [cljs-bean.core :as bean]))

;; sadly bean/->js and bean/->cljs do not allow specifying key transformation functions.
;; see https://github.com/mfikes/cljs-bean/issues/91
(defn clj
  ([x] (clj x
            :prop->key csk/->kebab-case-keyword
            :key->prop csk/->camelCaseString
            :recursive true))
  ([x & opts]
   (apply bean/bean x opts)))

(defn js
  [x]
  (clj->js x :keyword-fn csk/->camelCaseString))

(defn js-keys
  [x]
  ;; compared to `Object.keys`, this also returns the keys with `enumerable` being false.
  (js/Object.getOwnPropertyNames x))

(comment
  (js {:foo-bar {:foo-bar :bar}})

  (clj (js {:foo-bar {:foo-bar :bar}}))

  (.-fqn :ab/cd))

