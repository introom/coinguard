;; see api:
;; https://docs.hetzner.cloud/#images-get-all-images
(ns task.provider
  (:require
   [clojure.string :as str]
   [fpl.ops.hetzner :as h]
   [fpl.ops.config :refer [resource-name]]
   [fpl.clj.ssh :as ssh]
   [fpl.clj.process :refer [sh]]
   [task.script :as script]))

(def default-username "ops")

(defn ssh-cmd
  "Opens an ssh session to a host, e.g., `k ssh --host controller0-qa`."
  [{:keys [host username]
    :or {username default-username}
    :as ctx}]
  (let [public-ips (-> (h/get-servers)
                       h/server-name->public-ip)
        host (public-ips host)]
    (ssh/with-ssh {:host host :username username}
      (ssh/session))))

(defn script-cmd
  "Execute shell scripts on remote hosts.
   For example, `k script --host controller0-qa,worker0-qa --script install-deps`.
   See `task.script` for the script names."
  [{:keys [host username script]
    :or {username default-username}
    :as ctx}]
  (let [public-ips (-> (h/get-servers)
                       h/server-name->public-ip)]
    (doseq [h (str/split host #",")]
      (ssh/with-ssh {:host (public-ips h) :username username}
        (if-not script
          (ssh/session)
          (let [action-fn (requiring-resolve (symbol "task.script" script))
                action (action-fn ctx)]
            (ssh/script action)))))))

(defn k3s-controller-cmd
  [{:keys [controller kubeconfig username]
    :or {username default-username}
    :as ctx}]
  (assert kubeconfig)
  (let [servers (h/get-servers)
        public-ips (h/server-name->public-ip servers)
        private-ips (h/server-name->private-ip servers)
        controller-public-ip (public-ips controller)
        controller-private-ip (private-ips controller)]
    (ssh/with-ssh {:host controller-public-ip :username username}
      ;; install k3s controller
      (let [action (script/install-k3s-controller
                    {:node-ip controller-private-ip
                     :node-external-ip controller-public-ip})]
        (ssh/script action))
      ;; fetch kubeconfig file
      (let [proc (ssh/shell "sudo cat /etc/rancher/k3s/k3s.yaml" {:out :string})
            ;; update kubeconfig file
            replacement {"127.0.0.1" controller-public-ip
                         "localhost" controller-public-ip
                         "default" (resource-name "fpl-k8s")}
            output (-> (:out proc)
                       (str/replace #"127\.0\.0\.1|localhost|default"
                                    replacement))]
        (spit kubeconfig output)
        (sh (format "chmod 600 %s" kubeconfig))))))

(defn k3s-worker-cmd
  [{:keys [worker controller username]
    :or {username default-username}
    :as ctx}]
  (let [servers (h/get-servers)
        public-ips (h/server-name->public-ip servers)
        private-ips (h/server-name->private-ip servers)
        worker-public-ip (public-ips worker)
        worker-private-ip (private-ips worker)
        controller-public-ip (public-ips controller)
        controller-private-ip (private-ips controller)
        k3s-token (ssh/with-ssh {:host controller-public-ip :username username}
                    (-> (ssh/shell "sudo cat /var/lib/rancher/k3s/server/node-token" {:out :string})
                        :out
                        str/trim))]
    (ssh/with-ssh {:host worker-public-ip :username username}
      (let [action (script/install-k3s-worker {:controller-private-ip controller-private-ip
                                               :k3s-token k3s-token
                                               :node-ip worker-private-ip
                                               :node-external-ip worker-public-ip})]
        (ssh/script action)))))
