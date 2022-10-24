locals {
  # `hcloud image list`
  server_os = "debian-11"
  # `hcloud server-type list`
  controller_server_type = "cx21"
  controller_count       = 1
  worker_server_type     = "cx21"
  worker_count           = 1
}

# ssh key is manually added
data "hcloud_ssh_key" "default" {
  fingerprint = "7c:76:aa:c4:6c:6d:a4:88:05:e7:5b:c4:ff:77:f6:4d"
}

resource "hcloud_server" "controller" {
  count       = local.controller_count
  location    = local.location
  name        = "controller${count.index}-${local.env}"
  image       = local.server_os
  server_type = local.controller_server_type
  ssh_keys    = [data.hcloud_ssh_key.default.id]
  backups     = true
  labels = {
    type = "k8s-${local.env}"
  }
  user_data = file("./asset/user_data.yml")
}

resource "hcloud_server" "worker" {
  count       = local.controller_count
  location    = local.location
  name        = "worker${count.index}-${local.env}"
  image       = local.server_os
  server_type = local.controller_server_type
  ssh_keys    = [data.hcloud_ssh_key.default.id]
  backups     = true
  labels = {
    type = "k8s-${local.env}"
  }
  user_data = file("./asset/user_data.yml")
}
resource "hcloud_server_network" "controller" {
  count     = local.controller_count
  server_id = hcloud_server.controller[count.index].id
  subnet_id = hcloud_network_subnet.default.id
}

resource "hcloud_server_network" "worker" {
  count     = local.worker_count
  server_id = hcloud_server.worker[count.index].id
  subnet_id = hcloud_network_subnet.default.id
}
