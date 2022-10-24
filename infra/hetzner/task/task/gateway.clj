(ns task.gateway
  (:require
   [fpl.clj.process :refer [sh]]
   [fpl.clj.template :refer [render-path]]
   [fpl.ops.config :refer [resource-name]]
   [fpl.ops.kubernetes :refer [helm-set]]))

;; see https://kubernetes.github.io/ingress-nginx/deploy/#quick-start
;; see https://github.com/kubernetes/ingress-nginx/blob/main/charts/ingress-nginx/values.yaml
(def nginx-settings
  [["controller.extraArgs.default-ssl-certificate" "gateway/default-ssl-certificate"]])

(defn helm-install-nginx-cmd
  [_ctx]
  (let [name (resource-name "ingress-nginx")
        cmd
        (str "helm upgrade --install " name " ingress-nginx"
             " --repo https://kubernetes.github.io/ingress-nginx"
             " --namespace gateway --create-namespace"
             " " (helm-set nginx-settings))]
    (sh cmd)))

;; cert manager
;; see https://github.com/cert-manager/cert-manager/blob/master/deploy/charts/cert-manager/values.yaml
(def cert-manager-settings
  [["installCRDs" "true"]
   ;; see https://github.com/cert-manager/cert-manager/issues/4102#issuecomment-898593638
   ;; use the installation namespace `gateway`.
   ["global.leaderElection.namespace" "gateway"]])

(defn helm-install-cert-manager-cmd
  [_ctx]
  (let [name (resource-name "cert-manager")
        cmd
        (str "helm upgrade --install " name " cert-manager"
             " --repo https://charts.jetstack.io"
             " --namespace gateway --create-namespace"
             " " (helm-set cert-manager-settings))]
    (sh cmd)))

(defn kube-install-cert-manager-cmd
  [ctx]
  (let [content (doto (render-path "task/gateway/cert-manager.yml" ctx)
                  println)]
    (sh "kubectl apply -f -" {:in content})))