(ns app.exception)

(defn exception?
  [v]
  (instance? Throwable v))

(defmacro raise
  [msg & {:keys [cause] :as data}]
  ;; merge, we shall make sure data is non-nil
  (let [data (-> (merge {} data)
                 (dissoc :cause))]
    `(throw (ex-info ~msg ~data ~cause))))

(def unauthorized :unauthorized)
(def forbidden :forbidden)
(def invalid :invalid)
