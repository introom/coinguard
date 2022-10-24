(ns app.http.router
  (:require
   [app.api.swagger :as swagger]
   [app.http.error :refer [handle-error]]
   [app.api.account :as account]
   [app.api.session :as session]
   [app.api.wallet :as wallet]
   [app.http.middleware :as middleware]
   [reitit.ring :as ring]
   [reitit.coercion.malli :as coercion.malli]
   [reitit.ring.coercion :as coercion]
   [muuntaja.core :as m]
   [jsonista.core :as json]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.util.http-status :as status])
  (:import
   [com.fasterxml.jackson.annotation JsonInclude$Include]))

(def codec (m/create
            ;; given an unknown "accept" in request, the default-format will be used.
            ;; see https://github.com/metosin/muuntaja/blob/6d3fedbf48a21940b69b15989c374c63cf2ce26c/modules/muuntaja/src/muuntaja/core.clj#L244
            (assoc-in m/default-options
                      [:formats "application/json" :opts]
                      ;; the opts dict is merged with the default settings.  here the mapper is directly passed to objectmapper in jsonista.
                      ;; see https://cljdoc.org/d/metosin/muuntaja/0.6.8/doc/creating-new-formats#function-generators

                      ;; see https://github.com/metosin/muuntaja/blob/6d3fedbf48a21940b69b15989c374c63cf2ce26c/modules/muuntaja/src/muuntaja/format/json.clj#L15
                      ;; since we explicitly provide the mapper, so the default :decoder-opts is ignored!
                      {:mapper (-> (json/object-mapper {:decode-key-fn true})
                                   ;; strip off empty values in response
                                   ;; see https://www.logicbig.com/tutorials/misc/jackson/json-include-non-empty.html
                                   (.setSerializationInclusion JsonInclude$Include/NON_EMPTY))})))

(defn create-router
  [& {:keys [db session] :as ctx}]
  (ring/router
   [["/healthz" {:get {:summary "Helath check"
                       :swagger {:id "health-check"}
                       :handler (constantly {:status status/ok :body "Success"})}}]
    (swagger/router ctx)
    (session/router ctx)
    (account/router ctx)
    (wallet/router ctx)]
   {:data {;; we can create our own: https://cljdoc.org/d/metosin/reitit/0.5.15/doc/coercion/malli#configuring-coercion  
           :coercion coercion.malli/coercion
           :muuntaja codec
           ;; put format-middleware first to ensure clojure adt is (de)serialized.
           :middleware [muuntaja/format-middleware
                        middleware/server-timing
                        [middleware/error handle-error]
                        parameters/parameters-middleware
                        #_middleware/wrap-debug
                        coercion/coerce-exceptions-middleware
                        coercion/coerce-request-middleware
                        coercion/coerce-response-middleware
                        [middleware/session-data db (:event-ch session)]]}}))

;; dev
(comment
  (do
    (def *router (get-in user/system [:app.http.server/server :router]))

    (def *app (ring/ring-handler *router))

    (*app {:request-method :get :uri "/spec/api.json"})))

;; learning
(comment
  (require '[reitit.core :as r])
  (def router
    (r/router
     [["/api/ping/:id" ::ping]]))
  (r/match-by-path router "/api/ipa")
  (r/match-by-path router "/api/ping/asdf")
  (r/match-by-path router "/api/orders/3")
  (r/match-by-path router "/api/orders"))

;; scratch
(comment
  (re-find #"^/(healthz|docs)" "/docsasdf"))