(ns fpl.clj.http.default
  (:require
   [hato.client :as hc]))

;; XXX middleware modifies request/response such as headers
;; see https://github.com/gnarroway/hato/blob/ef829066afdaf458dd0299609e20162e65d32380/src/hato/middleware.clj#L711

;; see https://github.com/gnarroway/hato#building-a-client
(def default-client (hc/build-http-client {:connect-timeout 10000
                                           :redirect-policy :always}))
(def default-opts {;; http request
                   :content-type :json
                    ;; http response
                   :as :json :coerce :always})