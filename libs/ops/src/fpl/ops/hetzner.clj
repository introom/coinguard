;; see api:
;; https://docs.hetzner.cloud/#images-get-all-images
(ns fpl.ops.hetzner
  "The modules requires the environment variable `hcloud-token`."
  (:require
   [clojure.string :as str]
   [com.rpl.specter :as s]
   [fpl.clj.process :refer [get-env]]
   [fpl.clj.http :as http]))

(def ^:private ^:const hetzner-api-url "https://api.hetzner.cloud/v1")

(def hcloud-token-env :hcloud-token)

(defn- api-url [& paths]
  (str/join "/" (cons hetzner-api-url paths)))

(defn- request
  [method url & [opts]]
  (let [opts (merge {:oauth-token (get-env hcloud-token-env)}
                    opts)]
    (http/request method url opts)))

(defn get-servers
  []
  (println "Fetch server information.")
  (-> (request :get (api-url "servers"))
      :body :servers))

(defn server-name->public-ip
  [servers]
  (reduce (fn [m s]
            (let [name (:name s)
                  ip (get-in s [:public_net :ipv4 :ip])]
              (assoc m name ip)))
          {} servers))

(defn server-name->private-ip
  [servers]
  (reduce (fn [m s]
            (let [name (:name s)
                  ip (s/select-one [:private_net s/FIRST :ip] s)]
              (assoc m name ip)))
          {} servers))