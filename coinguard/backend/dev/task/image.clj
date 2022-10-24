;; see https://github.dev/djblue/portal/tree/master/dev/tasks
(ns task.image
  (:require
   [task.util :refer [sh git-hash-6]])
  (:import
   [java.time ZonedDateTime ZoneId]))

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- today
  "This function gives output such as `220201`."
  []
  (let [formatter (java.time.format.DateTimeFormatter/ofPattern "yyMMdd")
        date (ZonedDateTime/now (ZoneId/of "UTC"))]
    (.format date formatter)))

(defn image-name []
  (let [image "firepandalabs.azurecr.io/coinguard/backend"
        tag (git-hash-6)]
    (format "%s:%s" image tag)))

(def dockerfile "docker/backend/Dockerfile")

(defn build
  []
  (let [cmd [:docker :build
             :-t (image-name)
             :-f dockerfile
             :--platform :linux/amd64
             :.]]
    (sh cmd)))

(defn login
  []
  (sh "az acr login --name firepandalabs"))

(defn push
  []
  (login)
  (let [cmd [:docker :push
             (image-name)]]
    (sh cmd)))

(comment
  (build)
  (login)
  (push))
