(ns fpl.clj.ring
  (:require
   [reitit.coercion.malli :as coercion.malli]
   [muuntaja.core :as muuntaja]
   [jsonista.core :as json]
   [ring.adapter.jetty9 :as jetty])
  (:import
   [com.fasterxml.jackson.annotation JsonInclude$Include]))

(def ^:private default-server-opts
  {:send-server-version? false
   :join? false})

;; see https://ring-clojure.github.io/ring/ring.adapter.jetty.html
(defn start-server
  "`opts` accepts: `host`, `port`.
   See `jetty/run-jetty` for more configuration."
  [handler opts]
  (let [opts (merge default-server-opts opts)]
    (jetty/run-jetty handler opts)))

(defn stop-server
  [^org.eclipse.jetty.server.Server server]
  ;; wait since stop is async.
  (doto server .stop .join))

(def default-coercion coercion.malli/coercion)

(def default-codec
  (muuntaja/create
   ;; given an unknown "accept" in request, the default-format will be used.
   ;; see https://is.gd/elibij
   (assoc-in muuntaja/default-options
             [:formats "application/json" :opts]
             ;; the opts dict is merged with the default settings.  here the mapper is directly passed to objectmapper in jsonista.
             ;; see https://cljdoc.org/d/metosin/muuntaja/0.6.8/doc/creating-new-formats#function-generators
             ;; see https://is.gd/xucoso
             ;; since we explicitly provide the mapper, so the default :decoder-opts is ignored!
             {:mapper (-> (json/object-mapper {:decode-key-fn true})
                          ;; strip off empty values in response
                          ;; see https://www.logicbig.com/tutorials/misc/jackson/json-include-non-empty.html
                          (.setSerializationInclusion JsonInclude$Include/NON_EMPTY))})))
