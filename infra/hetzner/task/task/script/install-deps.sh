set -e
sudo apt-get -y update
# see issue: https://github.com/rancher/k3os/issues/702
# i encountered this error with v1.24.3+k3s1
sudo apt-get -y install apparmor