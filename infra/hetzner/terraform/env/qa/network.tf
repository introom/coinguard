locals {
  ip_range     = "10.1.0.0/16"
  network_zone = "eu-central"
}

resource "hcloud_network" "default" {
  name     = "default-${local.env}"
  ip_range = local.ip_range
}

resource "hcloud_network_subnet" "default" {
  network_id   = hcloud_network.default.id
  type         = "cloud"
  network_zone = local.network_zone
  ip_range     = local.ip_range
}
