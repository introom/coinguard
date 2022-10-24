(ns app.api.swagger
  (:require
   [reitit.swagger-ui :as ui]
   [reitit.swagger :as swagger]))

;; see https://swagger.io/docs/specification/2-0/api-host-and-base-path/ for reference

;; NB we are using swagger2.0 
;; see also https://cljdoc.org/d/metosin/reitit/0.5.15/doc/ring/swagger-support
(defn router [_]
  ["/spec" {:no-doc true}
   ["" (ui/create-swagger-ui-handler {:url "/api.json"})]
   ["/api.json" {:get {:swagger {:info {:title "Coinguard API"}
                                 :basePath "/"}
                       :handler (swagger/create-swagger-handler)}}]])