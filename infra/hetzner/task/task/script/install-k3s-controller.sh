set -e

# see https://rancher.com/docs/k3s/latest/en/quick-start/
# for the env setting, see https://get.k3s.io/
# and https://rancher.com/docs/k3s/latest/en/installation/install-options/server-config/

# `sudo k3s server --help` also lists available `INSTALL_K3S_EXEC` options.
curl -sfL https://get.k3s.io \
| INSTALL_K3S_EXEC="--disable=traefik --advertise-address {{node-ip}} --node-ip {{node-ip}} --node-external-ip {{node-external-ip}}" \
sh -
