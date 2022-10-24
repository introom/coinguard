output "server_controller_status" {
  value = {
    for server in hcloud_server.controller :
    server.name => [server.status, server.ipv4_address]
  }
}

output "server_worker_status" {
  value = {
    for server in hcloud_server.worker :
    server.name => [server.status, server.ipv4_address]
  }
}