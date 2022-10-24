;; see https://github.com/clojure/tools.namespace#warnings-for-aliases
;; the active repl ns (here we call refresh in 'user) is not reloaded.
;; therefore, we have to unalias the ns-alias
(doseq [sym '[main log]]
  (ns-unalias 'user sym))

(ns user
  (:require
   [app.main :as main]
   [app.logging :as log]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :as repl]
   [integrant.core :as ig]))

#_(repl/disable-reload! (find-ns 'integrant.core))

;; otherwise at the first `refresh`, all clj files on the classpath will be 
;; reloaded.
(repl/set-refresh-dirs "src/" "test/")

(defonce system nil)

(defn run-tests
  ([] (run-tests #"^app.*-test$"))
  ([target]
   (repl/refresh)
   (cond
     (instance? java.util.regex.Pattern target) (test/run-all-tests target)
     (symbol? target) (if-let [n (namespace target)]
                        (do (require (symbol n))
                            (test/test-vars [(resolve target)]))
                        (test/test-ns target)))))

(defn start
  []
  (ig/load-namespaces main/system-config)
  (alter-var-root #'system (fn [sys]
                             (when sys (ig/halt! sys))
                             (-> main/system-config
                                 ig/prep
                                 ig/init)))
  :started)

(defn stop []
  (alter-var-root #'system (fn [sys]
                             (when sys (ig/halt! sys))
                             nil))
  :stopped)

(defn reload-ns-and-start
  "Reload the ns because `repl/refresh` does not load the active ns in repl."
  []
  (load "/user")
  (start))

(defn restart []
  (stop)
  (repl/refresh :after 'user/reload-ns-and-start))

(comment
  (restart)
  (start)
  (stop))