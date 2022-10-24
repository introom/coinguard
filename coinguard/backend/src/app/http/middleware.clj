(ns app.http.middleware
  (:require
   [app.session :as session]
   [clojure.core.async :as a]
   [app.code :as code]
   [app.exception :as ex]))

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

(defn- wrap-session-data
  "Read session from header and update the `updated_at` column in db.
   through the session event channel."
  [handler db session-ch]
  (let [session-token "session-authorization"
        find-header (fn [headers ^String token]
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
        (ex/raise "access denied"
                  :type ex/unauthorized
                  :code code/access-denied))
      (handler req))))

(def session-auth
  {:name ::session-auth
   :compile (constantly wrap-session-auth)})

(defn wrap-error
  [handler on-error]
  (fn [req]
    (try
      (handler req)
      (catch Throwable e
        (on-error e req)))))

(def error
  {:name ::error
   :compile (constantly wrap-error)})

(defn wrap-debug
  [handler]
  (fn [req]
    (tap> {:debug req})
    (handler req)))

#_:clj-kondo/ignore
(def debug
  {:name ::debug
   :compile (constantly wrap-debug)})

(comment
  #_(accessrules/wrap-access-rules))