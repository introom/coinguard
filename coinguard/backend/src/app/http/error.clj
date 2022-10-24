(ns app.http.error
  (:require
   [app.logging :as log]
   [app.exception :as ex]
   [app.code :as code]
   [ring.util.http-status :as status]))

(defmulti handle-exception
  (fn [err & _rest]
    (let [edata (ex-data err)]
      (or (:type edata)
          (class err)))))

(defn- make-body
  [err]
  (assoc (ex-data err) :msg (ex-message err)))

(defmethod handle-exception ex/unauthorized
  [err _]
  {:status status/unauthorized
   :body (make-body err)})

(defmethod handle-exception ex/forbidden
  [err _]
  {:status status/forbidden
   :body (make-body err)})

(defmethod handle-exception ex/invalid
  [err _]
  {:status status/bad-request
   :body (make-body err)})

(defmethod handle-exception :default
  [err req]
  {:status status/internal-server-error
   :body (merge {:code code/internal-error}
                (make-body err))})

;; for the exact pg exception code, see
;; https://www.postgresql.org/docs/current/errcodes-appendix.html
(defmethod handle-exception org.postgresql.util.PSQLException
  [^java.sql.SQLException err req]
  (let [state (.getSQLState err)
        msg (ex-message err)
        body {:state state
              :msg msg}]
    (condp = state
      "57014"
      {:status status/gateway-timeout
       :body (merge body {:code code/db:timeout})}

      "23505"
      {:status status/bad-request
       :body (merge body {:code code/db:resource-already-exists})}

      ;; default clause when non matched.
      {:status status/internal-server-error
       :body (merge body {:code code/db:psql-error})})))

(defn handle-error
  [err req]
  ;; (log/info "Server errors" :err (ex-message err))
  (log/info "server errors" :err err)
  (if (some #(instance? % err)
            [java.util.concurrent.CompletionException
             java.util.concurrent.ExecutionException])
    (handle-exception (.getCause ^Throwable err) req)
    (handle-exception err req)))

(comment
  #_(Throwable->map))