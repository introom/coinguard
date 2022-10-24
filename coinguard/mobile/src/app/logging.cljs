(ns app.logging
  (:require
   [clojure.string :as str]
   [goog.log :as glog]
   [goog.string :as gstring]
   ["react-native" :as rn :refer (LogBox)])
  (:import goog.debug.Console)
  ;; see https://clojurescript.org/guides/ns-forms#_implicit_sugar
  (:require-macros app.logging))

(def levels
  {:error {:type goog.log.Level.SEVERE :func glog/error}
   :warn {:type goog.log.Level.WARNING :func glog/warning}
   :info {:type goog.log.Level.INFO :func glog/info}
   :debug {:type goog.log.Level.FINE :func glog/fine}})

(defonce console (Console.))

;; to tweak the formatter
;; see https://github.com/google/closure-library/blob/master/closure/goog/debug/formatter.js#L445
(defn- setup-console
  []
  (let [formatter #js {:formatRecord
                       (fn [^js record]
                         (.getMessage record))}]
    ;; see formatter source: https://is.gd/J61TBz
    (set! (.-formatter_ console) formatter)
    (.setCapturing console true)))

(defn- filter-js-console
  []
  (let [warn-fn js/console.warn
        msg-prefixes ["Got a component with the name"
                      "[react-native-gesture-handler] Seems like you"]]
    (set! js/console.warn
          (fn [msg & args]
            ;; see react-navigation: https://is.gd/qqTogi
            (when (or (not= (.-name (type msg)) "String")
                      (not-any? (partial str/starts-with? msg) msg-prefixes))
              (apply warn-fn msg args))))))

(defn setup
  [{:keys [level]}]
  ;; disable logbox.  
  ;; see https://reactnative.dev/docs/debugging#logbox
  (.ignoreAllLogs LogBox)
  ;; filter console.log/warn messages
  (filter-js-console)
  (glog/setLevel (glog/getLogger "app") (get-in levels [level :type]))
  (setup-console))

(defn format
  [_level time ns-name line msg keyvals]
  (gstring/format "[%s] ns:%s line:%s msg:%s %s"
                  time
                  ns-name
                  line
                  msg
                  (->> keyvals
                       (map #(str (name (first %)) ":" (second %)))
                       (str/join " "))))

(defn log
  [level ns-name line msg keyvals]
  (let [log-func (get-in levels [level :func])
        time (.toLocaleString (js/Date.) "en-US")
        output (format level time ns-name line msg keyvals)
        logger (glog/getLogger ns-name)]
    (log-func logger output)))

(comment
  (log :info "asdf" 32 "a" '(:foo "asdf"  :bar "asdf"))
  (log :error "asdf" 32 "a" '(:foo "asdf"  :bar "asdf"))
  (js/console.debug "asfd")
  (.-name (type "asdf"))
  (filter-js-console))
