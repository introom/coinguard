(ns fpl.clj.ring.middleware
  (:require
   [clojure.core.async :as a]
   [fpl.ring.session :as session]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [fpl.clj.exception :as ex]))

(def format-middleware muuntaja/format-middleware)

(def format-negotiate-middleware muuntaja/format-negotiate-middleware)
(def format-request-middleware muuntaja/format-request-middleware)
(def format-response-middleware muuntaja/format-response-middleware)

(def parameters-middleware parameters/parameters-middleware)

(def coerce-exceptions-middleware coercion/coerce-exceptions-middleware)
(def coerce-request-middleware coercion/coerce-request-middleware)
(def coerce-response-middleware coercion/coerce-response-middleware)

(defn- wrap-coercion-middleware
  [handler])

(def coercion-middleware
  {:name ::coercion-middleware
   :compile (constantly wrap-coercion-middleware)})

(defn- wrap-server-timing
  [handler]
  (let [seconds-from #(float (/ (- (System/nanoTime) %) 1000000000))]
    (fn [req]
      (let [start (System/nanoTime)
            resp (handler req)]
        (update resp :headers
                (fn [headers]
                  ;; see https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing#syntax
                  (assoc headers "Server-Timing" (str "total;dur=" (seconds-from start)))))))))

(def server-timing
  {:name ::server-timing
   :compile (constantly wrap-server-timing)})

(def ^:private session-token "session-authorization")

(defn- wrap-session-data
  "Reads session from header and update the `updated_at` column in db
   through the session event channel."
  [handler db session-ch]
  (let [find-header (fn [headers ^String token]
                      (some-> (filter #(.equalsIgnoreCase token (-> % key name)) headers)
                              first
                              val))
        header->session (fn [db {:keys [headers] :as req}]
                          (let [token (find-header headers session-token)]
                            ;; it is fine if no session matches
                            (try
                              (session/get-session db {:token token})
                              (catch Exception e))))]
    (fn [req]
      (if-let [{:keys [id account-id] :as session} (header->session db req)]
        (do
          (a/>!! session-ch id)
          (handler (assoc req :session {:id id :identity account-id})))
        (handler req)))))

(def session-data
  {:name ::session-data
   :compile (constantly wrap-session-data)})

(defn- wrap-session-auth
  [handler]
  (let [authenticated? (fn [req]
                         ;; the key is inserted in the middleware wrap-session-data
                         (get-in req [:session :identity]))]
    (fn [req]
      (when-not (authenticated? req)
        (ex/raise "Access denied"
                  :code ::session-unauthenticated))
      (handler req))))

(def session-auth
  {:name ::session-auth
   :compile (constantly wrap-session-auth)})

(defn wrap-exception
  [handler on-exception]
  (fn [req]
    (try
      (handler req)
      (catch Throwable e
        (on-exception e req)))))

(def exception
  {:name ::exception
   :compile (constantly wrap-exception)})

(defn wrap-tap-request
  [handler]
  (fn [req]
    (tap> {:ring-request req})
    (handler req)))

(def tap-request
  {:name ::tap-request
   :compile (constantly wrap-tap-request)})

