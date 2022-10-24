(require '[datomic.api :as d])
(require '[clojure.string :as string])

(defn launch-transactor []
  (-> (ProcessBuilder. ["bin/transactor" "config/transactor.properties"])
      .inheritIO
      .start
      .waitFor))

(future (launch-transactor))

(def databases ["default"])

(def db-uri-template "datomic:dev://localhost:4334/%s?password=datomic")

;; retry until transactor is up
;; recur cannot appear in a try-catch block, see https://stackoverflow.com/q/44719375/855160
(defmacro retry [& body]
  `(loop []
     (let [ex?# (try
                  ~@body
                  false
                  (catch Throwable _#
                    true))]
       (when ex?#
         ;; sleep 100ms
         (Thread/sleep 100)
         (recur)))))

(defn create-database []
  (doseq [db (map #(format db-uri-template %) databases)]
    ;; the transactor might not be up.  we simply retry.
    (retry
     (d/create-database db))))

(create-database)


(def db-option "-d %s,datomic:dev://localhost:4334/%s?password=datomic")

(defn launch-peer-server []
  (let [cmd (str "bin/run -m datomic.peer-server -h localhost -p 8998 -a datomic,datomic "
                 (string/join (map #(format db-option % %) databases)))]
    (-> (ProcessBuilder. (string/split cmd #" "))
        .inheritIO
        .start
        .waitFor)))

(future (launch-peer-server))

;; ;; because some non-daemon threads in the thread pool are running
;; ;; see https://clojuredocs.org/clojure.core/future#example-542692c9c026201cdc326a7b
;; (System/exit 0)
