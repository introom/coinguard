terraform {
  # see https://www.terraform.io/language/settings/backends/local#example-configuration
  backend "local" {
    path = "tfstate/terraform.tfstate"
  }
}