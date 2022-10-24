(ns tool.flyway
  "Migrate database.
   
   See `bin/ctl.clj` on how to calling this script (`bb` can also call this script).
   The migration sql scripts are stored in `resources/db/migration`.

   For debugging purpose, we can bypass this script and direclty run
   `flyway <command>`."
  (:require
   [environ.core :refer [env]]
   [app.logging :as log])
  (:import
   org.flywaydb.core.Flyway
   org.flywaydb.core.api.configuration.FluentConfiguration))

;; see https://flywaydb.org/documentation/usage/api/javadoc/org/flywaydb/core/api/configuration/FluentConfiguration.html
(defn- flyway
  ^Flyway
  [url user password location]
  (Flyway. (doto ^FluentConfiguration (FluentConfiguration.)
             (.dataSource url user password)
             ;; see also https://stackoverflow.com/a/11702298 on how to call java functions
             ;; with varargs.
             ;; the name comes from `(type (into-array ["some-str"]))`
             (.locations ^"[Ljava.lang.String;" (into-array String [location])))))

(defn migrate
  [{:keys [url user password location]
    :or {url (:flyway-url env)
         user (:flyway-user env)
         password (:flyway-password env)
         location (:flyway-locations env)}}]
  (log/info "Migrate database" :location location)
  (.migrate (flyway url user password location)))