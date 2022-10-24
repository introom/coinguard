## Steps to create the cluster

The `.envrc` environment variable file controls installation behavior.
Make sure it is well setup.

- under `terraform/env/{qa,prod}`, run terraform to create servers
- update `cloudflare` dns settings
- run `script` to install scripts
  ```shell
  k script --host controller0-qa,worker0-qa --script install-deps
  ```
- install k8s clusttr
  ```shell
  # the following is for qa.
  # the kubeconfig file is outputted to $KUBECONFIG
  k k3s-controller --controller controller0-qa
  # join as agent
  k k3s-worker --worker worker0-qa --controller controller0-qa
  ```
- install k8s facilities
  - nginx
    ```shell
    k helm-install-nginx
    ```
  - cert manager
    ```shell
    k helm-install-cert-manager
    k kube-install-cert-manager
    ```
- install example app `hello`
  ```shell
  k kube-install-hello
  ```