set -e

curl -sfL https://get.k3s.io | \
K3S_URL="https://{{controller-private-ip}}:6443" \
K3S_TOKEN="{{k3s-token}}" \
INSTALL_K3S_EXEC="--node-ip {{node-ip}} --node-external-ip {{node-external-ip}}" \
sh -
