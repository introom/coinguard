;; see also https://stripe.com/docs/api/errors on the high-level design.
(ns fpl.clj.exception
  (:refer-clojure :exclude [type]))

(defn exception?
  [v]
  (instance? Throwable v))

(defmacro raise
  "Usage:
   ```clojure
   (raise \"Invalid payment option\" :code ex-code/invalid-payment)
   ```
   The application should define a central registry:
   - app.exception.code: Error code.
   "
  [msg & {:keys [cause] :as data}]
  ;; merge, we shall make sure data is non-nil
  (let [data (-> (merge {} data)
                 (dissoc :cause))]
    `(throw (ex-info ~msg ~data ~cause))))

(defn code
  [err]
  (some-> err ex-data :code))

;; for pg, see https://www.postgresql.org/docs/current/errcodes-appendix.html
(defn sql-exception
  [^java.sql.SQLException err]
  {:state (.getSQLState err)
   :message (ex-message err)})