(ns fpl.clj.db.migration
  "Run `clojure -M -m fpl.clj.db.migration migrate` on the cli.

   Relevant environment variables are
   - `APP_DB_MIGRATION_URL`
   - `APP_DB_MIGRATION_USERNAME`
   - `APP_DB_MIGRATION_PASSWORD`
   
   For convenience, add the following to `deps.edn`.
   ```clojure
   :migration
   {:extra-deps
    {com.firepandalabs/clj.db {:git/url \"git @github.com:firepandalabs/clj.db.git\"
                               :git/sha \",,,\"}
    :main-opts
    [\"-m\" \"fpl.clj.db.migration\"]}
   ```"
  (:require
   [migratus.core :as migratus]
   [fpl.clj.cli :refer [dispatch]]
   [fpl.clj.process :refer [get-env]]))

;; see https://cljdoc.org/d/migratus/migratus/1.3.8/doc/readme#configuration
;; and https://github.com/seancorfield/next-jdbc/blob/develop/doc/getting-started.md#the-db-spec-hash-map
(def ^:dynamic *config*
  {:store :database
   :migration-dir "db/migration"
   :migration-table-name "db_migration"
   ;; this is the default separator
   :command-separator "--;;"
   ;; the jdbcUrl is printed.
   :db {:jdbcUrl (get-env :app-db-migration-url)
        :user (get-env :app-db-migration-username)
        :password (get-env :app-db-migration-password)}})

(defn- init
  "Initialize the data store by running the init script."
  [_m]
  (migratus/init *config*))

;; see https://cljdoc.org/d/migratus/migratus/1.3.8/doc/readme#generate-migration-files
(defn- create
  "Create a new migration with the current date"
  [{{:keys [file]} :opts :as m}]
  (migratus/create *config* file))

(defn- migrate
  "Bring up any migrations that are not completed.
   Returns nil if successful, :ignore if the table is reserved, :failure otherwise.
   Supports thread cancellation."
  [_m]
  (migratus/migrate *config*))

(defn- rollback
  "Runs down for the last migration that was run."
  [_m]
  (migratus/rollback *config*))

(defn rollback-until-just-after
  "Runs down all migrations after migration-id.
   This only considers completed migrations, and will not migrate up."
  [m]
  (let [id (-> m :opts :id parse-long)]
    (migratus/rollback-until-just-after *config* id)))

(defn migrate-until-just-before
  [m]
  (let [id (-> m :opts :id parse-long)]
    (migratus/migrate-until-just-before *config* id)))

(defn up
  [m]
  (let [ids (->> m :rest-cmds (map parse-long))]
    (apply migratus/up *config* ids)))

(defn down
  [m]
  (let [ids (->> m :rest-cmds (map parse-long))]
    (apply migratus/down *config* ids)))

(defn pending-list
  [m]
  (migratus/pending-list *config*))

(defn- help
  [_m]
  (println "No action sepcified. See the source code for help."))

(def ^:private dispatch-table
  [{:cmds ["init"] :fn init}
   {:cmds ["create"] :cmds-opts [:file] :fn create}
   {:cmds ["migrate"] :fn migrate}
   {:cmds ["rollback"] :fn rollback}
   {:cmds ["rollback-until-just-after"] :cmds-opts [:id] :fn rollback-until-just-after}
   {:cmds ["migrate-until-just-before"] :cmds-opts [:id] :fn migrate-until-just-before}
   {:cmds ["up"] :fn up}
   {:cmds ["down"] :fn down}
   {:cmds ["pending-list"] :fn pending-list}
   ;; catch all 
   {:cmds [] :fn help}])

(defn -main
  [& args]
  (dispatch dispatch-table args))
